package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourseTeamsCSVService {

  static final int NAMES_PER_COLUMN = 24;
  static final int TEAMS_PER_COLUMN = 4;

  @Autowired private RosterStudentRepository rosterStudentRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private CourseUniqueNameService courseUniqueNameService;

  public String buildTeamsCSV(Long courseId) {
    HashMap<Long, String> uniqueNames = courseUniqueNameService.getUniqueNames(courseId);

    List<RosterStudent> students =
        StreamSupport.stream(
                rosterStudentRepository.findByCourseId(courseId).spliterator(), false)
            .filter(s -> !s.getTeams().isEmpty())
            .sorted(Comparator.comparing(s -> uniqueNames.getOrDefault(s.getId(), "")))
            .collect(Collectors.toList());

    List<Team> teams =
        StreamSupport.stream(
                teamRepository.findByCourseIdOrderByNameAsc(courseId).spliterator(), false)
            .collect(Collectors.toList());

    StringBuilder sb = new StringBuilder();
    appendByNameSection(sb, students, uniqueNames);
    appendByTeamSection(sb, teams, uniqueNames);
    return sb.toString();
  }

  private void appendByNameSection(
      StringBuilder sb, List<RosterStudent> students, HashMap<Long, String> uniqueNames) {
    int numStudents = students.size();
    int numColumns = (int) Math.ceil((double) numStudents / NAMES_PER_COLUMN);

    sb.append("by name\n");

    for (int c = 0; c < numColumns; c++) {
      if (c > 0) sb.append(",,");
      sb.append("Name,Team");
    }
    if (numColumns > 0) sb.append("\n");

    for (int row = 0; row < NAMES_PER_COLUMN; row++) {
      boolean anyData = false;
      for (int col = 0; col < numColumns; col++) {
        if (row + col * NAMES_PER_COLUMN < numStudents) {
          anyData = true;
          break;
        }
      }
      if (!anyData) break;

      for (int col = 0; col < numColumns; col++) {
        if (col > 0) sb.append(",,");
        int idx = row + col * NAMES_PER_COLUMN;
        if (idx < numStudents) {
          RosterStudent student = students.get(idx);
          String name = uniqueNames.getOrDefault(student.getId(), "");
          String team = student.getTeams().stream().sorted().findFirst().orElse("");
          sb.append(name).append(",").append(team);
        } else {
          sb.append(",");
        }
      }
      sb.append("\n");
    }
  }

  private void appendByTeamSection(
      StringBuilder sb, List<Team> teams, HashMap<Long, String> uniqueNames) {
    int numTeams = teams.size();
    int numColumns = (int) Math.ceil((double) numTeams / TEAMS_PER_COLUMN);

    sb.append("by team\n");

    for (int c = 0; c < numColumns; c++) {
      if (c > 0) sb.append(",,");
      sb.append("Team,Name");
    }
    if (numColumns > 0) sb.append("\n");

    // Each layer corresponds to one team-slot per column
    for (int layer = 0; layer < TEAMS_PER_COLUMN; layer++) {
      List<Team> layerTeams = new ArrayList<>();
      for (int col = 0; col < numColumns; col++) {
        int teamIdx = layer + col * TEAMS_PER_COLUMN;
        layerTeams.add(teamIdx < numTeams ? teams.get(teamIdx) : null);
      }

      List<List<RosterStudent>> layerMembers = new ArrayList<>();
      int maxMembers = 0;
      for (Team team : layerTeams) {
        if (team == null) {
          layerMembers.add(List.of());
        } else {
          List<RosterStudent> members =
              team.getTeamMembers().stream()
                  .filter(tm -> tm.getRosterStudent() != null)
                  .map(tm -> tm.getRosterStudent())
                  .sorted(Comparator.comparing(s -> uniqueNames.getOrDefault(s.getId(), "")))
                  .collect(Collectors.toList());
          layerMembers.add(members);
          maxMembers = Math.max(maxMembers, members.size());
        }
      }

      for (int memberIdx = 0; memberIdx < maxMembers; memberIdx++) {
        for (int col = 0; col < numColumns; col++) {
          if (col > 0) sb.append(",,");
          Team team = layerTeams.get(col);
          List<RosterStudent> members = layerMembers.get(col);
          if (team != null && memberIdx < members.size()) {
            String name = uniqueNames.getOrDefault(members.get(memberIdx).getId(), "");
            sb.append(team.getName()).append(",").append(name);
          } else {
            sb.append(",");
          }
        }
        sb.append("\n");
      }
    }
  }
}
