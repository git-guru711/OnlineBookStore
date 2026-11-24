import { useState, type SubmitEvent } from 'react';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Paper from '@mui/material/Paper';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { register } from '@/features/auth/api/auth';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { safeRedirect } from '@/features/auth/utils/redirect';
import { emailError, fullNameError, newPasswordError, withoutEmpty } from '@/features/auth/utils/validation';
import { ApiError } from '@/lib/api-client';

interface RegisterErrors {
  fullName?: string;
  email?: string;
  password?: string;
}

/** Same rules as the backend's RegisterRequest. */
function validate(fullName: string, email: string, password: string): RegisterErrors {
  return withoutEmpty({
    fullName: fullNameError(fullName),
    email: emailError(email),
    password: newPasswordError(password),
  });
}

export function RegisterPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<RegisterErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    const found = validate(fullName, email, password);
    setErrors(found);
    if (Object.keys(found).length > 0) {
      return;
    }

    setFormError(null);
    setSubmitting(true);
    try {
      await register({ fullName: fullName.trim(), email: email.trim(), password });
      await login(email.trim(), password);
      navigate(safeRedirect(searchParams.get('redirect')), { replace: true });
    } catch (error) {
      showServerError(error);
    } finally {
      setSubmitting(false);
    }
  }

  /** Backend field errors go under the fields, anything else into the alert. */
  function showServerError(error: unknown) {
    if (error instanceof ApiError && error.type === 'email-already-registered') {
      setFormError('This email is already registered');
    } else if (error instanceof ApiError && Object.keys(error.fieldErrors).length > 0) {
      setErrors(error.fieldErrors);
    } else {
      setFormError(error instanceof Error ? error.message : 'Something went wrong. Please try again.');
    }
  }

  return (
    <Paper variant="outlined" sx={{ p: 4, maxWidth: 420, mx: 'auto' }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Create an account
      </Typography>
      {formError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {formError}
        </Alert>
      )}
      <Stack component="form" spacing={2} noValidate onSubmit={handleSubmit}>
        <TextField
          label="Full name"
          autoComplete="name"
          value={fullName}
          onChange={(event) => setFullName(event.target.value)}
          error={Boolean(errors.fullName)}
          helperText={errors.fullName}
        />
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
          autoComplete="new-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={Boolean(errors.password)}
          helperText={errors.password}
        />
        <Button type="submit" variant="contained" size="large" disabled={submitting}>
          Create account
        </Button>
      </Stack>
    </Paper>
  );
}
