import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Skeleton } from '../components/ui/skeleton';
import { User, CreditCard } from 'lucide-react';
import api from '../services/api';

const Settings = () => {
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  // Fetch subscription data
  const { data: subscription, isLoading: subLoading, isError: subError } = useQuery({
    queryKey: ['subscription'],
    queryFn: async () => {
      const response = await api.get('/api/subscriptions/user');
      return response.data;
    },
    retry: false,
    enabled: isAuthenticated,
  });

  const fullName = user?.fullName || 'N/A';
  const email = user?.email || 'N/A';
  const role = user?.role || 'N/A';

  const currentPlan = subscription?.planType || 'FREE';
  const planStatus = subscription?.status || 'Active';
  const expiryDate = subscription?.endDate
    ? new Date(subscription.endDate).toLocaleDateString()
    : 'N/A';

  const handleUpgrade = () => {
    navigate('/subscription');
  };

  return (
    <div className="space-y-6 max-w-2xl mx-auto">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Settings</h2>
        <p className="text-muted-foreground mt-2">Manage your account and subscription.</p>
      </div>

      {/* Profile Section */}
      <Card>
        <CardHeader className="flex flex-row items-center gap-4">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
            <User className="h-5 w-5 text-primary" />
          </div>
          <div>
            <CardTitle>Profile</CardTitle>
            <CardDescription>Your account information</CardDescription>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1">
              <p className="text-sm font-medium text-muted-foreground">Full Name</p>
              <p className="text-sm font-medium">{fullName}</p>
            </div>
            <div className="space-y-1">
              <p className="text-sm font-medium text-muted-foreground">Email</p>
              <p className="text-sm font-medium">{email}</p>
            </div>
            <div className="space-y-1">
              <p className="text-sm font-medium text-muted-foreground">Role</p>
              <Badge variant="secondary" className="uppercase">{role}</Badge>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Subscription Section */}
      <Card>
        <CardHeader className="flex flex-row items-center gap-4">
          <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
            <CreditCard className="h-5 w-5 text-primary" />
          </div>
          <div>
            <CardTitle>Subscription</CardTitle>
            <CardDescription>Your current plan and billing details</CardDescription>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {subLoading ? (
            <div className="space-y-3">
              <Skeleton className="h-4 w-1/3" />
              <Skeleton className="h-4 w-1/4" />
              <Skeleton className="h-4 w-1/2" />
            </div>
          ) : (
            <div className="grid gap-4 sm:grid-cols-3">
              <div className="space-y-1">
                <p className="text-sm font-medium text-muted-foreground">Current Plan</p>
                <Badge variant="secondary" className="uppercase">{currentPlan}</Badge>
              </div>
              <div className="space-y-1">
                <p className="text-sm font-medium text-muted-foreground">Status</p>
                <p className="text-sm font-medium">{planStatus}</p>
              </div>
              <div className="space-y-1">
                <p className="text-sm font-medium text-muted-foreground">Expiry Date</p>
                <p className="text-sm font-medium">{expiryDate}</p>
              </div>
            </div>
          )}
        </CardContent>
        <CardFooter>
          <Button onClick={handleUpgrade}>
            Upgrade Plan
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
};

export default Settings;