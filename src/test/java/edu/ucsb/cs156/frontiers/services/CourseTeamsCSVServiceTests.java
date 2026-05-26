package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.entities.Team;
import edu.ucsb.cs156.frontiers.entities.TeamMember;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import edu.ucsb.cs156.frontiers.repositories.TeamRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CourseTeamsCSVServiceTests {

  @Mock private RosterStudentRepository rosterStudentRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private CourseUniqueNameService courseUniqueNameService;

  @InjectMocks private CourseTeamsCSVService courseTeamsCSVService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private RosterStudent student(long id, String firstName, String lastName) {
    return RosterStudent.builder().id(id).firstName(firstName).lastName(lastName).build();
  }

  private TeamMember member(RosterStudent student, Team team) {
    return TeamMember.builder().rosterStudent(student).team(team).build();
  }

  // ── by name: single column ────────────────────────────────────────────────

  @Test
  public void test_by_name_section_single_column() {
    RosterStudent alice = student(1L, "ALICE", "SMITH");
    RosterStudent bob = student(2L, "BOB", "JONES");
    RosterStudent carol = student(3L, "CAROL", "LEE");

    Team team01 = Team.builder().id(1L).name("t-01").build();
    Team team02 = Team.builder().id(2L).name("t-02").build();

    TeamMember tm1 = member(alice, team01);
    TeamMember tm2 = member(bob, team01);
    TeamMember tm3 = member(carol, team02);

    alice.setTeamMembers(List.of(tm1));
    bob.setTeamMembers(List.of(tm2));
    carol.setTeamMembers(List.of(tm3));
    team01.setTeamMembers(List.of(tm1, tm2));
    team02.setTeamMembers(List.of(tm3));

    HashMap<Long, String> uniqueNames = new HashMap<>();
    uniqueNames.put(1L, "ALICE");
    uniqueNames.put(2L, "BOB");
    uniqueNames.put(3L, "CAROL");

    when(rosterStudentRepository.findByCourseId(1L)).thenReturn(List.of(alice, bob, carol));
    when(teamRepository.findByCourseIdOrderByNameAsc(1L)).thenReturn(List.of(team01, team02));
    when(courseUniqueNameService.getUniqueNames(1L)).thenReturn(uniqueNames);

    String csv = courseTeamsCSVService.buildTeamsCSV(1L);

    String expectedByName =
        """
        by name
        Name,Team
        ALICE,t-01
        BOB,t-01
        CAROL,t-02
        """;

    String expectedByTeam =
        """
        by team
        Team,Name
        t-01,ALICE
        t-01,BOB
        t-02,CAROL
        """;

    assertEquals(expectedByName + expectedByTeam, csv);
  }

  // ── by name: two columns (25 students triggers ceil(25/24) = 2 columns) ──

  @Test
  public void test_by_name_section_two_columns() {
    Team sharedTeam = Team.builder().id(1L).name("t-01").build();

    List<RosterStudent> students = new ArrayList<>();
    HashMap<Long, String> uniqueNames = new HashMap<>();
    for (int i = 1; i <= 25; i++) {
      RosterStudent s = student((long) i, String.format("NAME%02d", i), "LAST");
      TeamMember tm = member(s, sharedTeam);
      s.setTeamMembers(List.of(tm));
      students.add(s);
      uniqueNames.put((long) i, String.format("NAME%02d", i));
    }
    sharedTeam.setTeamMembers(
        students.stream().map(s -> s.getTeamMembers().get(0)).collect(Collectors.toList()));

    when(rosterStudentRepository.findByCourseId(1L)).thenReturn(students);
    when(teamRepository.findByCourseIdOrderByNameAsc(1L)).thenReturn(List.of());
    when(courseUniqueNameService.getUniqueNames(1L)).thenReturn(uniqueNames);

    String csv = courseTeamsCSVService.buildTeamsCSV(1L);
    String[] lines = csv.split("\n", -1);

    assertEquals("by name", lines[0]);
    assertEquals("Name,Team,,Name,Team", lines[1]);
    // Row 0: students[0]=NAME01 (col 0) and students[24]=NAME25 (col 1)
    assertEquals("NAME01,t-01,,NAME25,t-01", lines[2]);
    // Row 1: students[1]=NAME02 (col 0), no student at index 25 → trailing empty entry
    assertEquals("NAME02,t-01,,,", lines[3]);
  }

  // ── by name: student on multiple teams shows first alphabetically ─────────

  @Test
  public void test_student_on_multiple_teams_uses_first_alphabetically() {
    RosterStudent alice = student(1L, "ALICE", "SMITH");

    Team teamB = Team.builder().id(2L).name("t-02").build();
    Team teamA = Team.builder().id(1L).name("t-01").build();

    TeamMember tm1 = member(alice, teamB);
    TeamMember tm2 = member(alice, teamA);
    alice.setTeamMembers(List.of(tm1, tm2)); // t-02 listed before t-01

    teamA.setTeamMembers(List.of(tm2));
    teamB.setTeamMembers(List.of(tm1));

    HashMap<Long, String> uniqueNames = new HashMap<>();
    uniqueNames.put(1L, "ALICE");

    when(rosterStudentRepository.findByCourseId(1L)).thenReturn(List.of(alice));
    when(teamRepository.findByCourseIdOrderByNameAsc(1L)).thenReturn(List.of(teamA, teamB));
    when(courseUniqueNameService.getUniqueNames(1L)).thenReturn(uniqueNames);

    String csv = courseTeamsCSVService.buildTeamsCSV(1L);

    // t-01 comes before t-02 alphabetically — should appear in by-name section
    assertTrue(csv.startsWith("by name\nName,Team\nALICE,t-01\n"));
  }

  // ── by team: single column ────────────────────────────────────────────────

  @Test
  public void test_team_members_sorted_by_unique_name() {
    Team team = Team.builder().id(1L).name("t-01").build();

    RosterStudent charlie = student(3L, "CHARLIE", "BROWN");
    RosterStudent alice = student(1L, "ALICE", "SMITH");
    RosterStudent bob = student(2L, "BOB", "JONES");

    TeamMember tm1 = member(charlie, team);
    TeamMember tm2 = member(alice, team);
    TeamMember tm3 = member(bob, team);

    // intentionally non-alphabetical order on the team
    team.setTeamMembers(List.of(tm1, tm2, tm3));
    charlie.setTeamMembers(List.of(tm1));
    alice.setTeamMembers(List.of(tm2));
    bob.setTeamMembers(List.of(tm3));

    HashMap<Long, String> uniqueNames = new HashMap<>();
    uniqueNames.put(1L, "ALICE");
    uniqueNames.put(2L, "BOB");
    uniqueNames.put(3L, "CHARLIE");

    when(rosterStudentRepository.findByCourseId(1L))
        .thenReturn(List.of(charlie, alice, bob));
    when(teamRepository.findByCourseIdOrderByNameAsc(1L)).thenReturn(List.of(team));
    when(courseUniqueNameService.getUniqueNames(1L)).thenReturn(uniqueNames);

    String csv = courseTeamsCSVService.buildTeamsCSV(1L);

    String expectedByTeam =
        """
        by team
        Team,Name
        t-01,ALICE
        t-01,BOB
        t-01,CHARLIE
        """;

    assertTrue(csv.contains(expectedByTeam));
  }

  // ── by team: multiple columns (5 teams → ceil(5/4) = 2 columns) ──────────

  @Test
  public void test_by_team_section_multiple_columns() {
    RosterStudent s1 = student(1L, "ALICE", "A");
    RosterStudent s2 = student(2L, "BOB", "B");
    RosterStudent s3 = student(3L, "CAROL", "C");
    RosterStudent s4 = student(4L, "DAN", "D");
    RosterStudent s5 = student(5L, "EVE", "E");

    Team t1 = Team.builder().id(1L).name("t-01").build();
    Team t2 = Team.builder().id(2L).name("t-02").build();
    Team t3 = Team.builder().id(3L).name("t-03").build();
    Team t4 = Team.builder().id(4L).name("t-04").build();
    Team t5 = Team.builder().id(5L).name("t-05").build();

    TeamMember tm1 = member(s1, t1);
    TeamMember tm2 = member(s2, t2);
    TeamMember tm3 = member(s3, t3);
    TeamMember tm4 = member(s4, t4);
    TeamMember tm5 = member(s5, t5);

    s1.setTeamMembers(List.of(tm1));
    s2.setTeamMembers(List.of(tm2));
    s3.setTeamMembers(List.of(tm3));
    s4.setTeamMembers(List.of(tm4));
    s5.setTeamMembers(List.of(tm5));
    t1.setTeamMembers(List.of(tm1));
    t2.setTeamMembers(List.of(tm2));
    t3.setTeamMembers(List.of(tm3));
    t4.setTeamMembers(List.of(tm4));
    t5.setTeamMembers(List.of(tm5));

    HashMap<Long, String> uniqueNames = new HashMap<>();
    uniqueNames.put(1L, "ALICE");
    uniqueNames.put(2L, "BOB");
    uniqueNames.put(3L, "CAROL");
    uniqueNames.put(4L, "DAN");
    uniqueNames.put(5L, "EVE");

    when(rosterStudentRepository.findByCourseId(1L))
        .thenReturn(List.of(s1, s2, s3, s4, s5));
    when(teamRepository.findByCourseIdOrderByNameAsc(1L))
        .thenReturn(List.of(t1, t2, t3, t4, t5));
    when(courseUniqueNameService.getUniqueNames(1L)).thenReturn(uniqueNames);

    String csv = courseTeamsCSVService.buildTeamsCSV(1L);

    // numColumns=2: col 0 holds t1-t4 (layers 0-3), col 1 holds t5 (layer 0 only)
    // When col 1 is null for layers 1-3, output is: data_col0 + ",," + "," = three commas
    String expectedByTeam =
        "by team\n"
            + "Team,Name,,Team,Name\n"
            + "t-01,ALICE,,t-05,EVE\n"
            + "t-02,BOB,,,\n"
            + "t-03,CAROL,,,\n"
            + "t-04,DAN,,,\n";

    String expectedByName =
        "by name\nName,Team\nALICE,t-01\nBOB,t-02\nCAROL,t-03\nDAN,t-04\nEVE,t-05\n";

    assertEquals(expectedByName + expectedByTeam, csv);
  }

  // ── null roster student in team member is filtered out ────────────────────

  @Test
  public void test_null_roster_student_in_team_is_filtered() {
    Team team = Team.builder().id(1L).name("t-01").build();
    RosterStudent alice = student(1L, "ALICE", "SMITH");

    TeamMember realMember = member(alice, team);
    TeamMember nullMember = TeamMember.builder().rosterStudent(null).team(team).build();

    team.setTeamMembers(List.of(realMember, nullMember));
    alice.setTeamMembers(List.of(realMember));

    HashMap<Long, String> uniqueNames = new HashMap<>();
    uniqueNames.put(1L, "ALICE");

    when(rosterStudentRepository.findByCourseId(1L)).thenReturn(List.of(alice));
    when(teamRepository.findByCourseIdOrderByNameAsc(1L)).thenReturn(List.of(team));
    when(courseUniqueNameService.getUniqueNames(1L)).thenReturn(uniqueNames);

    String csv = courseTeamsCSVService.buildTeamsCSV(1L);

    // Only ALICE should appear; the null-rosterStudent member is silently skipped
    assertTrue(csv.contains("t-01,ALICE\n"));
    assertEquals(1, csv.lines().filter(l -> l.startsWith("t-01,")).count());
  }

  // ── students without teams excluded ──────────────────────────────────────

  @Test
  public void test_students_without_teams_excluded_from_by_name() {
    RosterStudent alice = student(1L, "ALICE", "SMITH");
    RosterStudent bob = student(2L, "BOB", "JONES");

    alice.setTeamMembers(List.of()); // no team
    bob.setTeamMembers(null);        // no team

    HashMap<Long, String> uniqueNames = new HashMap<>();
    uniqueNames.put(1L, "ALICE");
    uniqueNames.put(2L, "BOB");

    when(rosterStudentRepository.findByCourseId(1L)).thenReturn(List.of(alice, bob));
    when(teamRepository.findByCourseIdOrderByNameAsc(1L)).thenReturn(List.of());
    when(courseUniqueNameService.getUniqueNames(1L)).thenReturn(uniqueNames);

    String csv = courseTeamsCSVService.buildTeamsCSV(1L);

    assertEquals("by name\nby team\n", csv);
  }

  // ── empty course ──────────────────────────────────────────────────────────

  @Test
  public void test_empty_course_produces_section_headers_only() {
    when(rosterStudentRepository.findByCourseId(1L)).thenReturn(List.of());
    when(teamRepository.findByCourseIdOrderByNameAsc(1L)).thenReturn(List.of());
    when(courseUniqueNameService.getUniqueNames(1L)).thenReturn(new HashMap<>());

    String csv = courseTeamsCSVService.buildTeamsCSV(1L);

    assertEquals("by name\nby team\n", csv);
  }
}
