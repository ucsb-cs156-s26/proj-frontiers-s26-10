import { Button } from "react-bootstrap";

export default function DownloadsTabComponent({ courseId, testIdPrefix }) {
  const downloadStudentCsv = () => {
    window.open(`/api/csv/rosterstudents?courseId=${courseId}`, "_blank");
  };

  const downloadTeamsCsv = () => {
    window.open(`/api/csv/teams?courseId=${courseId}`, "_blank");
  };

  return (
    <div data-testid={`${testIdPrefix}-DownloadsTabComponent`}>
      <Button
        onClick={downloadStudentCsv}
        data-testid={`${testIdPrefix}-download-student-csv-button`}
        className="me-2"
      >
        Download Student CSV
      </Button>
      <Button
        onClick={downloadTeamsCsv}
        data-testid={`${testIdPrefix}-download-teams-csv-button`}
      >
        Download Teams CSV
      </Button>
    </div>
  );
}
