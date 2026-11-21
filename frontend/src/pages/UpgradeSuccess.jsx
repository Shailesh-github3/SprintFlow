import React, { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react';
import api from '../services/api';
import { Button } from '../components/ui/button';
import { Card, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/card';

const VALID_PLANS = ['MONTHLY', 'YEARLY'];

const UpgradeSuccess = () => {
  const [searchParams] = useSearchParams();
  const planType = searchParams.get('planType');
  const paymentStatus = searchParams.get('razorpay_payment_link_status'); // e.g., 'paid'
  const paymentId = searchParams.get('razorpay_payment_id');
  const paymentLinkId = searchParams.get('razorpay_payment_link_id') || searchParams.get('razorpay_order_id');
  const signature = searchParams.get('razorpay_signature');

  const [status, setStatus] = useState('processing'); // processing, success, error

  const verifyMutation = useMutation({
    mutationFn: (payload) => api.post('/api/payments/verify', payload),
    onSuccess: () => {
      setStatus('success');
    },
    onError: () => {
      setStatus('error');
    }
  });

  useEffect(() => {
    if (!planType || !VALID_PLANS.includes(planType)) {
      setStatus('error');
      return;
    }

    if (paymentStatus === 'failed') {
      setStatus('error');
      return;
    }

    // If Razorpay signature is present, verify via server-side HMAC validation
    if (paymentId && signature && paymentLinkId) {
      verifyMutation.mutate({
        planType,
        razorpayOrderId: paymentLinkId,
        razorpayPaymentId: paymentId,
        razorpaySignature: signature,
      });
    } else if (paymentStatus === 'paid') {
      // Test-mode simulated redirect without signature
      setStatus('success');
    } else {
      setStatus('error');
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [planType, paymentStatus, paymentId, paymentLinkId, signature]);

  return (
    <div className="flex h-[80vh] items-center justify-center p-4">
      <Card className="w-full max-w-md text-center">
        <CardHeader>
          <div className="flex justify-center mb-4">
            {status === 'processing' && <Loader2 className="h-12 w-12 text-primary animate-spin" />}
            {status === 'success' && <CheckCircle2 className="h-12 w-12 text-green-500" />}
            {status === 'error' && <XCircle className="h-12 w-12 text-destructive" />}
          </div>
          <CardTitle className="text-2xl">
            {status === 'processing' && 'Confirming Upgrade...'}
            {status === 'success' && 'Upgrade Successful!'}
            {status === 'error' && 'Upgrade Failed'}
          </CardTitle>
          <CardDescription>
            {status === 'processing' && 'Please wait while we verify your transaction.'}
            {status === 'success' && `You have been upgraded to the ${planType} plan.`}
            {status === 'error' && 'We could not verify your payment. Please try again or contact support.'}
          </CardDescription>
        </CardHeader>
        <CardFooter className="flex justify-center">
          {status !== 'processing' && (
            <Button asChild>
              <Link to="/subscription">Back to Subscription</Link>
            </Button>
          )}
        </CardFooter>
      </Card>
    </div>
  );
};

export default UpgradeSuccess;
