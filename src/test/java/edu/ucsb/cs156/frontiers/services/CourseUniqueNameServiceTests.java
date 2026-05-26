package edu.ucsb.cs156.frontiers.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.repositories.RosterStudentRepository;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CourseUniqueNameServiceTests {

  @Mock private RosterStudentRepository rosterStudentRepository;

  @InjectMocks private CourseUniqueNameService courseUniqueNameService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private RosterStudent student(long id, String firstName, String lastName) {
    return RosterStudent.builder().id(id).firstName(firstName).lastName(lastName).build();
  }

  @Test
  public void test_unique_short_names_need_no_disambiguation() {
    List<RosterStudent> students =
        List.of(
            student(1L, "ALICE JANE", "SMITH"),
            student(2L, "BOB HENRY", "JONES"),
            student(3L, "CAROL ANN", "LEE"));

    HashMap<Long, String> result = courseUniqueNameService.computeUniqueNames(students);

    assertEquals("ALICE", result.get(1L));
    assertEquals("BOB", result.get(2L));
    assertEquals("CAROL", result.get(3L));
  }

  @Test
  public void test_initial_disambiguates_duplicate_short_names() {
    // RICHARD BURTON and RICHARD JENKINS — initial disambiguates
    List<RosterStudent> students =
        List.of(student(1L, "RICHARD FRANCIS", "BURTON"), student(14L, "RICHARD DALE", "JENKINS"));

    HashMap<Long, String> result = courseUniqueNameService.computeUniqueNames(students);

    assertEquals("RICHARD B", result.get(1L));
    assertEquals("RICHARD J", result.get(14L));
  }

  @Test
  public void test_full_last_name_needed_when_initial_still_duplicates() {
    // JOHN LENNON, JOHN LEGEND, JOHN CUSACK — L is ambiguous for first two
    List<RosterStudent> students =
        List.of(
            student(11L, "JOHN WINSTON ONO", "LENNON"),
            student(12L, "JOHN ROGER", "LEGEND"),
            student(16L, "JOHN PAUL MARY", "CUSACK"));

    HashMap<Long, String> result = courseUniqueNameService.computeUniqueNames(students);

    assertEquals("JOHN LENNON", result.get(11L));
    assertEquals("JOHN LEGEND", result.get(12L));
    assertEquals("JOHN C", result.get(16L));
  }

  @Test
  public void test_full_spec_example() {
    List<RosterStudent> students =
        List.of(
            student(1L, "RICHARD FRANCIS", "BURTON"),
            student(2L, "ADELE LAURIE BLUE", "ADKINS"),
            student(3L, "HUGH JOHN MUNGO", "GRANT"),
            student(4L, "ELIZABETH ROSEMOND MARY", "TAYLOR"),
            student(5L, "HENRY DAVID", "THOREAU"),
            student(6L, "KIEFER WILLIAM FREDERICK ABBEY", "SUTHERLAND"),
            student(7L, "EMMA CHARLOTTE DUERRE", "WATSON"),
            student(8L, "JULIA SCARLETT ELIZABETH LOUIS", "DREYFUS"),
            student(9L, "BILLIE EILISH PIRATE BAIRD", "O'CONNELL"),
            student(10L, "WILLIAM BRADLEY", "PITT"),
            student(11L, "JOHN WINSTON ONO", "LENNON"),
            student(12L, "JOHN ROGER", "LEGEND"),
            student(13L, "LAURA JEANNE REESE", "WITHERSPOON"),
            student(14L, "RICHARD DALE", "JENKINS"),
            student(15L, "ELIZABETH IRENE", "BANKS"),
            student(16L, "JOHN PAUL MARY", "CUSACK"));

    HashMap<Long, String> result = courseUniqueNameService.computeUniqueNames(students);

    assertEquals("RICHARD B", result.get(1L));
    assertEquals("ADELE", result.get(2L));
    assertEquals("HUGH", result.get(3L));
    assertEquals("ELIZABETH T", result.get(4L));
    assertEquals("HENRY", result.get(5L));
    assertEquals("KIEFER", result.get(6L));
    assertEquals("EMMA", result.get(7L));
    assertEquals("JULIA", result.get(8L));
    assertEquals("BILLIE", result.get(9L));
    assertEquals("WILLIAM", result.get(10L));
    assertEquals("JOHN LENNON", result.get(11L));
    assertEquals("JOHN LEGEND", result.get(12L));
    assertEquals("LAURA", result.get(13L));
    assertEquals("RICHARD J", result.get(14L));
    assertEquals("ELIZABETH B", result.get(15L));
    assertEquals("JOHN C", result.get(16L));
  }

  @Test
  public void test_getUniqueNames_delegates_to_repository() {
    List<RosterStudent> students =
        List.of(student(1L, "ALICE", "SMITH"), student(2L, "BOB", "JONES"));

    when(rosterStudentRepository.findByCourseId(42L)).thenReturn(students);

    HashMap<Long, String> result = courseUniqueNameService.getUniqueNames(42L);

    assertEquals("ALICE", result.get(1L));
    assertEquals("BOB", result.get(2L));
  }

  @Test
  public void test_null_first_name_handled() {
    List<RosterStudent> students = List.of(student(1L, null, "SMITH"));

    HashMap<Long, String> result = courseUniqueNameService.computeUniqueNames(students);

    assertEquals("", result.get(1L));
  }

  @Test
  public void test_blank_first_name_handled() {
    List<RosterStudent> students = List.of(student(1L, "  ", "JONES"));

    HashMap<Long, String> result = courseUniqueNameService.computeUniqueNames(students);

    assertEquals("", result.get(1L));
  }

  @Test
  public void test_empty_student_list_returns_empty_map() {
    HashMap<Long, String> result = courseUniqueNameService.computeUniqueNames(List.of());

    assertEquals(0, result.size());
  }

  @Test
  public void test_null_last_name_during_disambiguation() {
    // Two JOHNs with null last names — initial is "" for both, so full last name is also ""
    // Per spec: leave as duplicate
    List<RosterStudent> students =
        List.of(student(1L, "JOHN PAUL", null), student(2L, "JOHN ROGER", null));

    HashMap<Long, String> result = courseUniqueNameService.computeUniqueNames(students);

    // Both end up with "JOHN " (short name + space + empty last name) — intentional duplicate
    assertEquals(result.get(1L), result.get(2L));
  }

  @Test
  public void test_single_student_uses_short_name() {
    List<RosterStudent> students = List.of(student(1L, "ALICE JANE", "SMITH"));

    HashMap<Long, String> result = courseUniqueNameService.computeUniqueNames(students);

    assertEquals("ALICE", result.get(1L));
  }
}
