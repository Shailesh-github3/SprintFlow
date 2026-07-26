import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Mail, Users, Tag, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';
import api from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Skeleton } from '../components/ui/skeleton';
import { Avatar, AvatarFallback } from '../components/ui/avatar';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '../components/ui/dialog';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import IssueList from '../components/IssueList';
import ProjectChat from '../components/ProjectChat';

const ProjectDetails = () => {
  const { projectId } = useParams();
  const { user, isAuthenticated } = useAuth();
  const queryClient = useQueryClient();
  const [isInviteOpen, setIsInviteOpen] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');

  // Fetch Project
  const { data: project, isLoading, isError } = useQuery({
    queryKey: ['projects', projectId],
    queryFn: async () => {
      const response = await api.get(`/api/projects/${projectId}`);
      return response.data;
    },
    enabled: isAuthenticated && !!projectId,
  });

  // Invite Member Mutation
  const inviteMutation = useMutation({
    mutationFn: (email) => api.post('/api/projects/invite', { projectId: Number(projectId), userEmail: email }),
    onSuccess: () => {
      setIsInviteOpen(false);
      setInviteEmail('');
      toast.success('Invitation sent successfully');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to send invitation');
    }
  });

  const handleInvite = (e) => {
    e.preventDefault();
    if (!inviteEmail) return;
    inviteMutation.mutate(inviteEmail);
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-1/4" />
        <Card><CardHeader><Skeleton className="h-6 w-1/2" /></CardHeader><CardContent><Skeleton className="h-20 w-full" /></CardContent></Card>
      </div>
    );
  }

  if (isError || !project) {
    return (
      <div className="text-center text-destructive p-12 border rounded-lg border-dashed">
        <AlertCircle className="h-8 w-8 mx-auto mb-2" />
        Failed to load project details.
      </div>
    );
  }

  const isOwner = project.projectOwner?.email === user?.email;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="outline" size="icon" asChild>
          <Link to="/projects"><ArrowLeft className="h-4 w-4" /></Link>
        </Button>
        <div>
          <h2 className="text-3xl font-bold tracking-tight">{project.projectName}</h2>
          <p className="text-muted-foreground flex items-center gap-2 mt-1">
            <Tag className="h-4 w-4" /> {project.category}
          </p>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-3">
        {/* Main Details */}
        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle>Project Details</CardTitle>
          </CardHeader>
          <CardContent className="space-y-6">
            <div>
              <h3 className="text-sm font-medium text-muted-foreground mb-1">Description</h3>
              <p className="text-sm leading-relaxed whitespace-pre-wrap">{project.projectDescription}</p>
            </div>
            
            {project.tags && project.tags.length > 0 && (
              <div>
                <h3 className="text-sm font-medium text-muted-foreground mb-2">Tags</h3>
                <div className="flex flex-wrap gap-2">
                  {project.tags.map((tag, i) => (
                    <Badge variant="secondary" key={i}>{tag}</Badge>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Team Members */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0">
            <div>
              <CardTitle>Team</CardTitle>
              <CardDescription>Members of this project</CardDescription>
            </div>
            <Users className="h-5 w-5 text-muted-foreground" />
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Project Owner */}
            <div className="flex items-center gap-3">
              <Avatar>
                <AvatarFallback>{project.projectOwner?.fullName?.charAt(0) || 'O'}</AvatarFallback>
              </Avatar>
              <div>
                <p className="text-sm font-medium leading-none">{project.projectOwner?.fullName}</p>
                <p className="text-xs text-muted-foreground">Owner</p>
              </div>
            </div>

            {/* Team Members */}
            {project.projectMembers?.map((member) => (
              <div key={member.userId} className="flex items-center gap-3">
                <Avatar>
                  <AvatarFallback>{member.fullName?.charAt(0) || 'M'}</AvatarFallback>
                </Avatar>
                <div>
                  <p className="text-sm font-medium leading-none">{member.fullName}</p>
                  <p className="text-xs text-muted-foreground">Member</p>
                </div>
              </div>
            ))}

            {isOwner && (
              <Button variant="outline" className="w-full mt-4 gap-2" onClick={() => setIsInviteOpen(true)}>
                <Mail className="h-4 w-4" />
                Invite Member
              </Button>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Invite Dialog */}
      <Dialog open={isInviteOpen} onOpenChange={setIsInviteOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>Invite Team Member</DialogTitle>
            <DialogDescription>
              Enter the email address of the user you want to invite to this project.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleInvite} className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="email">Email address</Label>
              <Input 
                id="email" 
                type="email" 
                placeholder="user@example.com" 
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
                required
                disabled={inviteMutation.isPending}
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button variant="outline" type="button" onClick={() => setIsInviteOpen(false)} disabled={inviteMutation.isPending}>
                Cancel
              </Button>
              <Button type="submit" disabled={inviteMutation.isPending}>
                {inviteMutation.isPending ? 'Sending...' : 'Send Invite'}
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* Tabs for Issues and Chat */}
      <Tabs defaultValue="issues" className="mt-8">
        <TabsList className="mb-4">
          <TabsTrigger value="issues">Issues</TabsTrigger>
          <TabsTrigger value="chat">Team Chat</TabsTrigger>
        </TabsList>
        <TabsContent value="issues">
          <IssueList projectId={projectId} />
        </TabsContent>
        <TabsContent value="chat">
          <div className="max-w-4xl mx-auto">
            <ProjectChat projectId={projectId} />
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default ProjectDetails;