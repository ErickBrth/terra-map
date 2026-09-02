import { useState, useCallback } from 'react';
import { reserveParcel, markParcelSold } from '../../../api/landParcelApi';
import { ApiError } from '../../../api/ApiError';
import { updateFeatureStatus } from '../../map/layers/parcelLayer';

export function useUpdateParcelStatus(onDone: (newStatus: string) => void) {
  const [pending, setPending] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const run = useCallback(
    async (id: string, action: (id: string) => Promise<{ status: string }>) => {
      setPending(true);
      setErrorMessage(null);
      try {
        const updated = await action(id);
        // Updates the map immediately (colour change) without waiting for a
        // fresh search or reload — the popup already has everything it needs.
        updateFeatureStatus(id, updated.status);
        onDone(updated.status);
      } catch (error) {
        setErrorMessage(error instanceof ApiError ? error.userMessage : 'Something went wrong. Please try again.');
      } finally {
        setPending(false);
      }
    },
    [onDone],
  );

  const reserve = useCallback((id: string) => run(id, reserveParcel), [run]);
  const markSold = useCallback((id: string) => run(id, markParcelSold), [run]);

  return { reserve, markSold, pending, errorMessage };
}
