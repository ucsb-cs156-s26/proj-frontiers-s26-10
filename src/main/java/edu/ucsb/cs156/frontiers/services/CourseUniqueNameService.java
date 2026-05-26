package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourseUniqueNameService {

  @Autowired private RosterStudentRepository rosterStudentRepository;

  public HashMap<Long, String> getUniqueNames(Long courseId) {
    List<RosterStudent> students =
        StreamSupport.stream(
                rosterStudentRepository.findByCourseId(courseId).spliterator(), false)
            .collect(Collectors.toList());
    return computeUniqueNames(students);
  }

  public HashMap<Long, String> computeUniqueNames(List<RosterStudent> students) {
    Map<Long, String> shortNames = new HashMap<>();
    for (RosterStudent s : students) {
      String fn = s.getFirstName();
      String shortName = (fn != null && !fn.isBlank()) ? fn.strip().split("\\s+")[0] : "";
      shortNames.put(s.getId(), shortName);
    }

    Map<String, List<RosterStudent>> byShortName =
        students.stream().collect(Collectors.groupingBy(s -> shortNames.get(s.getId())));

    HashMap<Long, String> result = new HashMap<>();

    for (Map.Entry<String, List<RosterStudent>> entry : byShortName.entrySet()) {
      String shortName = entry.getKey();
      List<RosterStudent> group = entry.getValue();

      if (group.size() == 1) {
        result.put(group.get(0).getId(), shortName);
      } else {
        // Try disambiguating with first initial of last name
        Map<String, List<RosterStudent>> byInitial =
            group.stream()
                .collect(
                    Collectors.groupingBy(
                        s -> {
                          String ln = s.getLastName();
                          String initial =
                              (ln != null && !ln.isEmpty()) ? String.valueOf(ln.charAt(0)) : "";
                          return shortName + " " + initial;
                        }));

        for (Map.Entry<String, List<RosterStudent>> initialEntry : byInitial.entrySet()) {
          String withInitial = initialEntry.getKey();
          List<RosterStudent> initialGroup = initialEntry.getValue();

          if (initialGroup.size() == 1) {
            result.put(initialGroup.get(0).getId(), withInitial);
          } else {
            // Use full last name; if still duplicate, leave as-is per spec
            for (RosterStudent s : initialGroup) {
              String ln = s.getLastName() != null ? s.getLastName() : "";
              result.put(s.getId(), shortName + " " + ln);
            }
          }
        }
      }
    }

    return result;
  }
}
