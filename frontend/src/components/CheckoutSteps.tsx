import Step from '@mui/material/Step';
import StepLabel from '@mui/material/StepLabel';
import Stepper from '@mui/material/Stepper';

const STEPS = ['Cart', 'Review', 'Done'] as const;

interface CheckoutStepsProps {
  /** 1 = Review, 3 = all done. */
  active: number;
}

export function CheckoutSteps({ active }: CheckoutStepsProps) {
  return (
    <Stepper activeStep={active} alternativeLabel aria-label="Checkout progress" sx={{ mb: 4 }}>
      {STEPS.map((label) => (
        <Step key={label}>
          <StepLabel>{label}</StepLabel>
        </Step>
      ))}
    </Stepper>
  );
}
