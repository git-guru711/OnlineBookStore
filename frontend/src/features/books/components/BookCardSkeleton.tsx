import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Skeleton from '@mui/material/Skeleton';

export function BookCardSkeleton() {
  return (
    <Card variant="outlined" aria-hidden>
      <Skeleton variant="rectangular" height={210} animation="wave" />
      <CardContent>
        <Skeleton width="80%" height={28} />
        <Skeleton width="50%" />
        <Skeleton width="30%" height={32} sx={{ mt: 1 }} />
        <Skeleton variant="rounded" width={110} height={36} sx={{ mt: 2, borderRadius: 999 }} />
      </CardContent>
    </Card>
  );
}
