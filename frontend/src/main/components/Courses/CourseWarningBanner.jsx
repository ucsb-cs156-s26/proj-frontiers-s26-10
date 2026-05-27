import { Alert, Button } from "react-bootstrap";
import { useBackend, useBackendMutation } from "main/utils/useBackend";

const objectToAxiosParams = (courseId) => ({
  url: `/api/courses/hidePermissionWarning?courseId=${courseId}`,
  method: "POST",
});

export function CourseWarningBanner({ courseId, orgName }) {
  const { data: warnings } = useBackend(
    [`/api/courses/warnings/${courseId}`],
    {
      method: "GET",
      url: `/api/courses/warnings/${courseId}`,
    },
    undefined,
    true,
    {
      placeholderData: { showOrganizationAgeWarning: false },
      staleTime: "static",
    },
  );

  const hideWarningMutation = useBackendMutation(
    () => objectToAxiosParams(courseId),
    {},
    // Stryker disable next-line ArrayDeclaration,StringLiteral : cache key string equality is an implementation detail
    [`/api/courses/warnings/${courseId}`],
  );

  const permission = warnings?.defaultBasePermission;
  const showPermissionWarning =
    permission &&
    permission !== "none" &&
    permission !== "null" &&
    // Stryker disable next-line OptionalChaining : placeholder data ensures warnings is never null
    !warnings?.hideBasePermissionWarning;

  return (
    <>
      {warnings?.showOrganizationAgeWarning && (
        <Alert variant="warning">
          Warning: This GitHub Organization is less than 30 days old. You will
          experience difficulties enrolling more than 50 students in a day.
        </Alert>
      )}
      {showPermissionWarning && (
        <Alert variant="warning">
          Warning: the organization setting for Default Base Permission is not
          the recommended value of None. This means that students in the
          organization may be able to access other students&apos; private
          repos.&nbsp;
          {orgName && (
            <Alert.Link
              href={`https://github.com/organizations/${orgName}/settings/member_privileges`}
              target="_blank"
              rel="noopener noreferrer"
            >
              You can change that setting here
            </Alert.Link>
          )}
          &nbsp;
          <Button
            variant="warning"
            size="sm"
            data-testid="CourseWarningBanner-dismiss-button"
            onClick={() => hideWarningMutation.mutate()}
          >
            Dismiss
          </Button>
        </Alert>
      )}
    </>
  );
}
