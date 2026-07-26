import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Check, Loader2, Sparkles } from 'lucide-react';
import { toast } from 'sonner';
import api from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';

const Subscription = () => {
  const [selectedPlan, setSelectedPlan] = useState(null);
  const { isAuthenticated } = useAuth();

  // Fetch current subscription
  const { data: subscription, isLoading, isError } = useQuery({
    queryKey: ['subscription'],
    queryFn: async () => {
      const response = await api.get('/api/subscriptions/user');
      return response.data;
    },
    enabled: isAuthenticated,
  });

  // Create payment link mutation
  const paymentMutation = useMutation({
    mutationFn: (planType) => api.post(`/api/payments/${planType}`),
    onSuccess: (data) => {
      // Redirect to Razorpay checkout URL
      if (data.data && data.data.paymentLink) {
        window.location.href = data.data.paymentLink;
      } else {
        toast.error('Failed to get payment link from server.');
      }
      setSelectedPlan(null);
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to initialize payment');
      setSelectedPlan(null);
    }
  });

  const handleUpgrade = (planType) => {
    setSelectedPlan(planType);
    paymentMutation.mutate(planType);
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="text-center text-destructive p-8 border rounded-lg border-dashed">
        Failed to load subscription details.
      </div>
    );
  }

  const currentPlan = subscription?.planType || 'FREE';

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      <div className="text-center mb-10 mt-6">
        <h2 className="text-3xl font-bold tracking-tight mb-2">Manage Your Subscription</h2>
        <div className="text-muted-foreground flex items-center justify-center gap-1 flex-wrap">
          You are currently on the <Badge variant="secondary" className="ml-1 uppercase">{currentPlan}</Badge> plan.
          {subscription?.endDate && (
            <span className="ml-2 block mt-1 text-xs">
              Valid until: {new Date(subscription.endDate).toLocaleDateString()}
            </span>
          )}
        </div>
      </div>

      <div className="grid md:grid-cols-3 gap-6">
        {/* FREE PLAN */}
        <Card className={`relative flex flex-col ${currentPlan === 'FREE' ? 'border-primary shadow-sm' : ''}`}>
          {currentPlan === 'FREE' && (
            <div className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-primary text-primary-foreground text-xs font-bold px-3 py-1 rounded-full">
              CURRENT PLAN
            </div>
          )}
          <CardHeader>
            <CardTitle>Free</CardTitle>
            <CardDescription>Perfect for getting started</CardDescription>
            <div className="mt-4">
              <span className="text-4xl font-bold">₹0</span>
              <span className="text-muted-foreground">/forever</span>
            </div>
          </CardHeader>
          <CardContent className="flex-1">
            <ul className="space-y-3 text-sm">
              <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> Up to 3 projects</li>
              <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> Basic task management</li>
              <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> Community support</li>
            </ul>
          </CardContent>
          <CardFooter>
            <Button variant="outline" className="w-full" disabled={currentPlan === 'FREE'}>
              {currentPlan === 'FREE' ? 'Active' : 'Downgrade'}
            </Button>
          </CardFooter>
        </Card>

        {/* MONTHLY PLAN */}
        <Card className={`relative flex flex-col border-primary shadow-md ${currentPlan === 'MONTHLY' ? 'ring-2 ring-primary ring-offset-2' : ''}`}>
          {currentPlan === 'MONTHLY' && (
            <div className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-primary text-primary-foreground text-xs font-bold px-3 py-1 rounded-full">
              CURRENT PLAN
            </div>
          )}
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-amber-500" /> Pro Monthly
              </CardTitle>
            </div>
            <CardDescription>For growing teams</CardDescription>
            <div className="mt-4">
              <span className="text-4xl font-bold">₹9.99</span>
              <span className="text-muted-foreground">/month</span>
            </div>
          </CardHeader>
          <CardContent className="flex-1">
            <ul className="space-y-3 text-sm">
              <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> Unlimited projects</li>
              <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> Advanced task management</li>
              <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> Team chat & collaboration</li>
              <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> Priority support</li>
            </ul>
          </CardContent>
          <CardFooter>
            <Button 
              className="w-full" 
              onClick={() => handleUpgrade('MONTHLY')}
              disabled={currentPlan === 'MONTHLY' || paymentMutation.isPending}
            >
              {paymentMutation.isPending && selectedPlan === 'MONTHLY' ? (
                <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Processing...</>
              ) : currentPlan === 'MONTHLY' ? 'Active' : 'Upgrade to Monthly'}
            </Button>
          </CardFooter>
        </Card>

        {/* YEARLY PLAN */}
        <Card className={`relative flex flex-col ${currentPlan === 'YEARLY' ? 'border-primary shadow-sm ring-2 ring-primary ring-offset-2' : ''}`}>
          {currentPlan === 'YEARLY' && (
            <div className="absolute top-0 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-primary text-primary-foreground text-xs font-bold px-3 py-1 rounded-full">
              CURRENT PLAN
            </div>
          )}
          <CardHeader>
            <CardTitle>Pro Yearly</CardTitle>
            <CardDescription>Best value for long-term</CardDescription>
            <div className="mt-4">
              <span className="text-4xl font-bold">₹99.99</span>
              <span className="text-muted-foreground">/year</span>
            </div>
            <Badge variant="secondary" className="w-fit mt-1 text-green-600 bg-green-100 dark:bg-green-900/30">Save ~17%</Badge>
          </CardHeader>
          <CardContent className="flex-1">
            <ul className="space-y-3 text-sm">
              <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> Everything in Monthly</li>
              <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> Dedicated account manager</li>
              <li className="flex items-center gap-2"><Check className="h-4 w-4 text-green-500" /> Custom branding</li>
            </ul>
          </CardContent>
          <CardFooter>
            <Button 
              variant={currentPlan === 'YEARLY' ? "outline" : "default"}
              className="w-full" 
              onClick={() => handleUpgrade('YEARLY')}
              disabled={currentPlan === 'YEARLY' || paymentMutation.isPending}
            >
              {paymentMutation.isPending && selectedPlan === 'YEARLY' ? (
                <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Processing...</>
              ) : currentPlan === 'YEARLY' ? 'Active' : 'Upgrade to Yearly'}
            </Button>
          </CardFooter>
        </Card>
      </div>
    </div>
  );
};

export default Subscription;
