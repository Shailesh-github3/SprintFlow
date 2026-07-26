import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react';
import api from '../services/api';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/card';

const UpgradeSuccess = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const planType = searchParams.get('planType');
  const paymentId = searchParams.get('razorpay_payment_id'); // Razorpay usually adds this
  const paymentStatus = searchParams.get('razorpay_payment_link_status'); // e.g., 'paid'
  
  const [status, setStatus] = useState('processing'); // processing, success, error

  const upgradeMutation = useMutation({
    mutationFn: () => api.patch(`/api/subscriptions/update/${planType}`),
    onSuccess: () => {
      setStatus('success');
    },
    onError: () => {
      setStatus('error');
    }
  });

  useEffect(() => {
    if (!planType) {
      setStatus('error');
      return;
    }

    // In a real application, you'd verify the payment signature on the backend via a webhook or 
    // a verification endpoint before upgrading the user.
    // Here we assume if they hit this callback (and optionally check status), it's successful 
    // based on the backend prototype specification.
    
    if (paymentStatus === 'failed') {
      setStatus('error');
    } else {
      upgradeMutation.mutate();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [planType, paymentStatus]);

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
            {status === 'processing' && 'Processing Upgrade...'}
            {status === 'success' && 'Upgrade Successful!'}
            {status === 'error' && 'Upgrade Failed'}
          </CardTitle>
          <CardDescription>
            {status === 'processing' && 'Please wait while we confirm your payment and update your account.'}
            {status === 'success' && `You have successfully upgraded to the ${planType} plan.`}
            {status === 'error' && 'We could not process your payment or upgrade at this time. Please try again.'}
          </CardDescription>
        </CardHeader>
        
        {status !== 'processing' && (
          <CardFooter className="flex flex-col gap-2">
            <Button className="w-full" asChild>
              <Link to="/subscription">Back to Subscription</Link>
            </Button>
            <Button variant="outline" className="w-full" asChild>
              <Link to="/">Go to Dashboard</Link>
            </Button>
          </CardFooter>
        )}
      </Card>
    </div>
  );
};

export default UpgradeSuccess;
