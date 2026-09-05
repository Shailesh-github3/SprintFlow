import React, { useEffect, useState } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import api from '../services/api';
import { Button } from '../components/ui/button';
import { Card, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/card';

// Landing page for project invitation email links (?token=...).
// Accepts the invitation for the currently logged-in user.
const InviteAccept = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [status, setStatus] = useState('processing'); // processing, success, error
  const [message, setMessage] = useState('Joining project...');

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setMessage('This invitation link is missing its token.');
      return;
    }
    api.get('/api/projects/invite', { params: { token } })
      .then(() => {
        setStatus('success');
        toast.success('Invitation accepted');
        setTimeout(() => navigate('/projects'), 1500);
      })
      .catch((error) => {
        setStatus('error');
        setMessage(error.response?.data?.message || 'This invitation is invalid or has expired.');
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

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
            {status === 'processing' && 'Accepting Invitation...'}
            {status === 'success' && 'Welcome to the project!'}
            {status === 'error' && 'Invitation Failed'}
          </CardTitle>
          <CardDescription>
            {status === 'processing' && message}
            {status === 'success' && 'Redirecting to your projects...'}
            {status === 'error' && message}
          </CardDescription>
        </CardHeader>

        {status !== 'processing' && (
          <CardFooter className="flex flex-col gap-2">
            <Button className="w-full" asChild>
              <Link to="/projects">Go to Projects</Link>
            </Button>
          </CardFooter>
        )}
      </Card>
    </div>
  );
};

export default InviteAccept;
