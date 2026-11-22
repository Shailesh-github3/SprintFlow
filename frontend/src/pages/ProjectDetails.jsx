import React, { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Mail, Users, Tag, AlertCircle, UserMinus, LogOut, BarChart3, CheckCircle2, Clock, AlertTriangle } from 'lucide-react';
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
import ActivityFeed from '../components/ActivityFeed';
import ConfirmDialog from '../components/ConfirmDialog';

const ProjectDetails = () => {
  const { projectId } = useParams();
  const { user, isAuthenticated } = useAuth();
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const [isInviteOpen, setIsInviteOpen] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');

  const [removeTarget, setRemoveTarget] = useState(null);
  const [isLeaveOpen, setIsLeaveOpen] = useState(false);

  // Fetch Project
  const { data: project, isLoading, isError } = useQuery({
    queryKey: ['projects', projectId],
    queryFn: async () => {
      const response = await api.get(`/api/projects/${projectId}`);
      return response.data;
    },
    enabled: isAuthenticated && !!projectId,
  });

  // Fetch Project Stats for Reports Tab
  const { data: stats, isLoading: isStatsLoading } = useQuery({
    queryKey: ['projects', projectId, 'stats'],
    queryFn: async () => {
      const response = await api.get(`/api/projects/${projectId}/stats`);
      return response.data;
    },
    enabled: isAuthenticated && !!projectId,
  });

  // Invite Member Mutation
  const inviteMutation = useMutation({
    mutationFn: (email) => api.post('/api/projects/invite', { projectId: Number(projectId), userEmail: email }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects', projectId] });
      queryClient.invalidateQueries({ queryKey: ['activity', projectId] });
      setIsInviteOpen(false);
      setInviteEmail('');
      toast.success('Invitation sent successfully');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to send invitation');
    }
  });

  // Remove Member Mutation
  const removeMutation = useMutation({
    mutationFn: (targetUserId) => api.delete(`/api/projects/${projectId}/members/${targetUserId}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects', projectId] });
      queryClient.invalidateQueries({ queryKey: ['activity', projectId] });
      queryClient.invalidateQueries({ queryKey: ['issues', projectId] });
      setRemoveTarget(null);
      toast.success('Member removed from project');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to remove member');
    }
  });

  // Leave Project Mutation
  const leaveMutation = useMutation({
    mutationFn: () => api.post(`/api/projects/${projectId}/leave`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      setIsLeaveOpen(false);
      toast.success('You have left the project');
      navigate('/projects');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to leave project');
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
  const isMember = project.projectMembers?.some((m) => m.email === user?.email);
  const canLeave = isMember && !isOwner;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="outline" size="icon" asChild>
          <Link to="/projects"><ArrowLeft className="h-4 w-4" /></Link>
        </Button>
        <div>
          <h2 className="text-3xl font-bold tracking-tight">{project.projectName}</h2>
          <p className="text-muted-foreground flex items-center gap-2 mt-1 text-sm">
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
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-3">
            <div>
              <CardTitle className="text-base">Team Members</CardTitle>
              <CardDescription className="text-xs">Collaborators on this project</CardDescription>
            </div>
            <Users className="h-5 w-5 text-muted-foreground" />
          </CardHeader>
          <CardContent className="space-y-3">
            {/* Project Owner */}
            <div className="flex items-center justify-between border-b pb-2">
              <div className="flex items-center gap-2.5">
                <Avatar className="h-8 w-8">
                  <AvatarFallback className="text-xs bg-primary/10 text-primary font-bold">
                    {project.projectOwner?.fullName?.charAt(0) || 'O'}
                  </AvatarFallback>
                </Avatar>
                <div>
                  <p className="text-sm font-medium leading-none">{project.projectOwner?.fullName}</p>
                  <p className="text-[11px] text-muted-foreground mt-0.5">{project.projectOwner?.email}</p>
                </div>
              </div>
              <Badge variant="secondary" className="text-[10px] px-1.5 py-0 font-normal">Owner</Badge>
            </div>

            {/* Team Members */}
            {project.projectMembers?.filter(m => (m.userId || m.id) !== (project.projectOwner?.userId || project.projectOwner?.id)).map((member) => {
              const memberId = member.userId || member.id;
              return (
                <div key={memberId} className="flex items-center justify-between py-1">
                  <div className="flex items-center gap-2.5">
                    <Avatar className="h-8 w-8">
                      <AvatarFallback className="text-xs">
                        {member.fullName?.charAt(0) || 'M'}
                      </AvatarFallback>
                    </Avatar>
                    <div>
                      <p className="text-sm font-medium leading-none">{member.fullName}</p>
                      <p className="text-[11px] text-muted-foreground mt-0.5">{member.email}</p>
                    </div>
                  </div>

                  {isOwner && (
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-7 w-7 text-muted-foreground hover:text-destructive"
                      title="Remove member"
                      onClick={() => setRemoveTarget(member)}
                    >
                      <UserMinus className="h-3.5 w-3.5" />
                    </Button>
                  )}
                </div>
              );
            })}

            {isOwner && (
              <Button variant="outline" className="w-full mt-4 gap-2 text-xs" onClick={() => setIsInviteOpen(true)}>
                <Mail className="h-3.5 w-3.5" />
                Invite Member
              </Button>
            )}

            {canLeave && (
              <Button
                variant="ghost"
                className="w-full mt-2 gap-2 text-xs text-destructive hover:bg-destructive/10"
                onClick={() => setIsLeaveOpen(true)}
              >
                <LogOut className="h-3.5 w-3.5" />
                Leave Project
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
              Enter the email address of the registered user you want to invite.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleInvite} className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="email">Email address</Label>
              <Input 
                id="email" 
                type="email" 
                placeholder="colleague@example.com" 
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

      {/* Remove Member Confirmation */}
      <ConfirmDialog
        open={!!removeTarget}
        setOpen={(open) => !open && setRemoveTarget(null)}
        title="Remove team member?"
        description={`Are you sure you want to remove ${removeTarget?.fullName} (${removeTarget?.email}) from this project? Their assigned tasks in this project will be unassigned.`}
        confirmLabel="Remove member"
        isLoading={removeMutation.isPending}
        onConfirm={() => removeTarget && removeMutation.mutate(removeTarget.userId || removeTarget.id)}
      />

      {/* Leave Project Confirmation */}
      <ConfirmDialog
        open={isLeaveOpen}
        setOpen={setIsLeaveOpen}
        title="Leave this project?"
        description="Are you sure you want to leave this project? Your assigned tasks will be unassigned."
        confirmLabel="Leave project"
        isLoading={leaveMutation.isPending}
        onConfirm={() => leaveMutation.mutate()}
      />

      {/* Tabs for Issues, Chat, Activity Feed, and Reports */}
      <Tabs defaultValue="issues" className="mt-8">
        <TabsList className="mb-4">
          <TabsTrigger value="issues">Issues</TabsTrigger>
          <TabsTrigger value="chat">Team Chat</TabsTrigger>
          <TabsTrigger value="activity">Activity</TabsTrigger>
          <TabsTrigger value="reports">Reports</TabsTrigger>
        </TabsList>

        <TabsContent value="issues">
          <IssueList projectId={projectId} project={project} />
        </TabsContent>

        <TabsContent value="chat">
          <div className="max-w-4xl mx-auto">
            <ProjectChat projectId={projectId} />
          </div>
        </TabsContent>

        <TabsContent value="activity">
          <ActivityFeed projectId={projectId} />
        </TabsContent>

        <TabsContent value="reports">
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-xl font-bold tracking-tight">Project Health & Reports</h3>
                <p className="text-xs text-muted-foreground mt-0.5">Real-time completion metrics and progress analytics</p>
              </div>
            </div>

            {isStatsLoading ? (
              <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
                {[1, 2, 3, 4].map(i => (
                  <Card key={i}><CardHeader><Skeleton className="h-4 w-1/2" /></CardHeader><CardContent><Skeleton className="h-8 w-1/4" /></CardContent></Card>
                ))}
              </div>
            ) : !stats ? (
              <div className="text-center text-muted-foreground p-8 border rounded-lg border-dashed">
                No stats available for this project.
              </div>
            ) : (
              <div className="space-y-6">
                <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-5">
                  <Card>
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                      <CardTitle className="text-xs font-medium text-muted-foreground">Total Issues</CardTitle>
                      <BarChart3 className="h-4 w-4 text-muted-foreground" />
                    </CardHeader>
                    <CardContent>
                      <div className="text-2xl font-bold">{stats.totalIssues}</div>
                      <p className="text-[11px] text-muted-foreground mt-1">All logged tasks</p>
                    </CardContent>
                  </Card>

                  <Card>
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                      <CardTitle className="text-xs font-medium text-muted-foreground">To Do</CardTitle>
                      <Clock className="h-4 w-4 text-yellow-500" />
                    </CardHeader>
                    <CardContent>
                      <div className="text-2xl font-bold">{stats.todoIssues}</div>
                      <p className="text-[11px] text-muted-foreground mt-1">Pending kickoff</p>
                    </CardContent>
                  </Card>

                  <Card>
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                      <CardTitle className="text-xs font-medium text-muted-foreground">In Progress</CardTitle>
                      <Clock className="h-4 w-4 text-blue-500" />
                    </CardHeader>
                    <CardContent>
                      <div className="text-2xl font-bold">{stats.inProgressIssues}</div>
                      <p className="text-[11px] text-muted-foreground mt-1">Actively worked</p>
                    </CardContent>
                  </Card>

                  <Card>
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                      <CardTitle className="text-xs font-medium text-muted-foreground">Done</CardTitle>
                      <CheckCircle2 className="h-4 w-4 text-green-500" />
                    </CardHeader>
                    <CardContent>
                      <div className="text-2xl font-bold">{stats.completedIssues}</div>
                      <p className="text-[11px] text-muted-foreground mt-1">Successfully resolved</p>
                    </CardContent>
                  </Card>

                  <Card className={stats.overdueIssues > 0 ? "border-destructive/40 bg-destructive/5" : ""}>
                    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                      <CardTitle className="text-xs font-medium text-muted-foreground">Overdue</CardTitle>
                      <AlertTriangle className={`h-4 w-4 ${stats.overdueIssues > 0 ? "text-destructive" : "text-muted-foreground"}`} />
                    </CardHeader>
                    <CardContent>
                      <div className={`text-2xl font-bold ${stats.overdueIssues > 0 ? "text-destructive" : ""}`}>{stats.overdueIssues}</div>
                      <p className="text-[11px] text-muted-foreground mt-1">Past due date</p>
                    </CardContent>
                  </Card>
                </div>

                {/* Progress bar card */}
                <Card>
                  <CardHeader className="pb-3">
                    <CardTitle className="text-sm font-semibold">Completion Rate</CardTitle>
                    <CardDescription className="text-xs">Overall completion ratio of project deliverables</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-2">
                    <div className="flex items-center justify-between text-sm">
                      <span className="font-bold text-lg">{stats.completionPercentage}%</span>
                      <span className="text-xs text-muted-foreground">{stats.completedIssues} of {stats.totalIssues} completed</span>
                    </div>
                    <div className="w-full bg-muted rounded-full h-3 overflow-hidden">
                      <div
                        className="bg-primary h-3 rounded-full transition-all duration-500"
                        style={{ width: `${Math.min(100, Math.max(0, stats.completionPercentage))}%` }}
                      ></div>
                    </div>
                  </CardContent>
                </Card>
              </div>
            )}
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default ProjectDetails;
