import React, { useState } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { CheckCircle2, XCircle, Loader2, MailCheck, UserPlus, X } from 'lucide-react';
import { toast } from 'sonner';
import api from '../services/api';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/card';

// Landing page for project invitation links (?token=...).
// Allows accepting or declining the invitation for the currently logged-in user.
const InviteAccept = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');

  const [status, setStatus] = useState('prompt'); // prompt, accepting, rejecting, success, declined, error
  const [message, setMessage] = useState('');

  const handleAccept = async () => {
    if (!token) return;
    setStatus('accepting');
    try {
      await api.get('/api/projects/invite', { params: { token } });
      setStatus('success');
      toast.success('Invitation accepted successfully');
      setTimeout(() => navigate('/projects'), 1500);
    } catch (error) {
      setStatus('error');
      setMessage(error.response?.data?.message || 'This invitation is invalid or has expired.');
    }
  };

  const handleReject = async () => {
    if (!token) return;
    setStatus('rejecting');
    try {
      await api.post(`/api/projects/invite/reject?token=${encodeURIComponent(token)}`);
      setStatus('declined');
      toast.info('Invitation declined');
      setTimeout(() => navigate('/projects'), 1500);
    } catch (error) {
      setStatus('error');
      setMessage(error.response?.data?.message || 'Failed to decline invitation.');
    }
  };

  if (!token) {
    return (
      <div className="flex h-[80vh] items-center justify-center p-4">
        <Card className="w-full max-w-md text-center">
          <CardHeader>
            <div className="flex justify-center mb-4">
              <XCircle className="h-12 w-12 text-destructive" />
            </div>
            <CardTitle className="text-2xl">Missing Invitation Token</CardTitle>
            <CardDescription>
              This invitation link is missing its verification token.
            </CardDescription>
          </CardHeader>
          <CardFooter>
            <Button className="w-full" asChild>
              <Link to="/projects">Go to Projects</Link>
            </Button>
          </CardFooter>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex h-[80vh] items-center justify-center p-4">
      <Card className="w-full max-w-md text-center shadow-lg">
        <CardHeader>
          <div className="flex justify-center mb-4">
            {status === 'prompt' && <MailCheck className="h-12 w-12 text-primary" />}
            {(status === 'accepting' || status === 'rejecting') && (
              <Loader2 className="h-12 w-12 text-primary animate-spin" />
            )}
            {status === 'success' && <CheckCircle2 className="h-12 w-12 text-green-500" />}
            {status === 'declined' && <XCircle className="h-12 w-12 text-muted-foreground" />}
            {status === 'error' && <XCircle className="h-12 w-12 text-destructive" />}
          </div>

          <CardTitle className="text-2xl">
            {status === 'prompt' && 'Project Invitation'}
            {status === 'accepting' && 'Joining Project...'}
            {status === 'rejecting' && 'Declining Invitation...'}
            {status === 'success' && 'Welcome to the Team!'}
            {status === 'declined' && 'Invitation Declined'}
            {status === 'error' && 'Invitation Error'}
          </CardTitle>

          <CardDescription className="mt-1">
            {status === 'prompt' && 'You have been invited to collaborate on a SprintFlow project.'}
            {status === 'accepting' && 'Confirming your membership...'}
            {status === 'rejecting' && 'Removing invitation...'}
            {status === 'success' && 'Redirecting to your projects workspace...'}
            {status === 'declined' && 'The invitation was declined. Redirecting...'}
            {status === 'error' && message}
          </CardDescription>
        </CardHeader>

        <CardContent>
          {status === 'prompt' && (
            <div className="flex flex-col gap-3 pt-2">
              <Button onClick={handleAccept} className="w-full gap-2">
                <UserPlus className="h-4 w-4" />
                Accept & Join Project
              </Button>
              <Button onClick={handleReject} variant="outline" className="w-full gap-2 text-destructive hover:bg-destructive/10">
                <X className="h-4 w-4" />
                Decline Invitation
              </Button>
            </div>
          )}
        </CardContent>

        {status === 'error' && (
          <CardFooter>
            <Button className="w-full" asChild>
              <Link to="/projects">Return to Projects</Link>
            </Button>
          </CardFooter>
        )}
      </Card>
    </div>
  );
};

export default InviteAccept;
