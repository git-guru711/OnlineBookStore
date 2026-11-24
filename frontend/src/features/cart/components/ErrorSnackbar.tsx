import Alert from '@mui/material/Alert';
import Portal from '@mui/material/Portal';
import Snackbar from '@mui/material/Snackbar';

interface ErrorSnackbarProps {
  message: string | null;
  onClose: () => void;
}

/** Stays until closed. In a Portal, so a transformed parent cannot move it from the screen corner. */
export function ErrorSnackbar({ message, onClose }: ErrorSnackbarProps) {
  return (
    <Portal>
      <Snackbar open={message !== null} onClose={onClose}>
        <Alert severity="error" variant="filled" onClose={onClose}>
          {message}
        </Alert>
      </Snackbar>
    </Portal>
  );
}
