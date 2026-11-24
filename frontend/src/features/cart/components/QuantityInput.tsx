import { useState } from 'react';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';

/** The backend's @Max(99). */
const MAX_PER_LINE = 99;

interface QuantityInputProps {
  title: string;
  value: number;
  available: number;
  onChange: (quantity: number) => void;
}

/** Presentational. A typed value applies on blur, so typing "12" asks once, not for 1 and then 12. */
export function QuantityInput({ title, value, available, onChange }: QuantityInputProps) {
  const [draft, setDraft] = useState<string | null>(null);
  const max = Math.min(MAX_PER_LINE, available);

  function applyDraft() {
    if (draft !== null) {
      const typed = Number(draft);
      if (Number.isInteger(typed) && typed >= 1 && typed <= max && typed !== value) {
        onChange(typed);
      }
    }
    setDraft(null);
  }

  return (
    <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}>
      <IconButton
        size="small"
        aria-label={`Decrease quantity of ${title}`}
        disabled={value <= 1}
        onClick={() => onChange(value - 1)}
      >
        <RemoveIcon fontSize="small" />
      </IconButton>
      <TextField
        size="small"
        value={draft ?? String(value)}
        onChange={(event) => setDraft(event.target.value)}
        onBlur={applyDraft}
        sx={{ width: 56 }}
        slotProps={{
          htmlInput: { 'aria-label': `Quantity of ${title}`, inputMode: 'numeric', style: { textAlign: 'center' } },
        }}
      />
      <IconButton
        size="small"
        aria-label={`Increase quantity of ${title}`}
        disabled={value >= max}
        onClick={() => onChange(value + 1)}
      >
        <AddIcon fontSize="small" />
      </IconButton>
    </Stack>
  );
}
