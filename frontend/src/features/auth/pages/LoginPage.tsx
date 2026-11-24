import { useState, type SubmitEvent } from 'react';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { safeRedirect } from '@/features/auth/utils/redirect';
import { emailError, withoutEmpty } from '@/features/auth/utils/validation';
import { ApiError } from '@/lib/api-client';

interface LoginErrors {
  email?: string;
  password?: string;
}

function validate(email: string, password: string): LoginErrors {
  return withoutEmpty({
    email: emailError(email),
    password: password ? undefined : 'Enter your password', // existing accounts: only "not empty"
  });
}

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<LoginErrors>({});

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const found = validate(email, password);
    setErrors(found);
    if (Object.keys(found).length > 0) {
      return;
    }

    setFormError(null);
    setSubmitting(true);
    try {
      await login(email.trim(), password);
      navigate(safeRedirect(searchParams.get('redirect')), { replace: true });
    } catch (error) {
      // vague on purpose: never say whether the email or the password was wrong
      setFormError(
        error instanceof ApiError && error.status === 401
          ? 'Invalid email or password'
          : error instanceof Error
            ? error.message
            : 'Something went wrong. Please try again.',
      );
      setSubmitting(false);
    }
  }

  return (
    <Paper variant="outlined" sx={{ p: 4, maxWidth: 420, mx: 'auto' }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Log in
      </Typography>
      {formError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {formError}
        </Alert>
      )}
      {/* noValidate: our messages instead of the browser's pop-ups */}
      <Stack component="form" spacing={2} noValidate onSubmit={handleSubmit}>
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={Boolean(errors.email)}
          helperText={errors.email}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={Boolean(errors.password)}
          helperText={errors.password}
        />
        <Button type="submit" variant="contained" size="large" disabled={submitting}>
          Log in
        </Button>
      </Stack>
    </Paper>
  );
}
