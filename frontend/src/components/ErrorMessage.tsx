import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Button from '@mui/material/Button';

interface ErrorMessageProps {
  title: string;
  message: string;
  onRetry?: () => void;
}

/** MUI Alert has role="alert", so screen readers announce it. */
export function ErrorMessage({ title, message, onRetry }: ErrorMessageProps) {
  return (
    <Alert
      severity="error"
      action={
        onRetry && (
          <Button color="inherit" size="small" onClick={onRetry}>
            Try again
          </Button>
        )
      }
    >
      <AlertTitle>{title}</AlertTitle>
      {message}
    </Alert>
  );
}
