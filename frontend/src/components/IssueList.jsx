import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2, Calendar, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';
import api from '../services/api';
import { Button } from './ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardFooter } from './ui/card';
import { Badge } from './ui/badge';
import { Skeleton } from './ui/skeleton';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from './ui/dropdown-menu';
import IssueForm from './IssueForm';
import IssueDetailsDialog from './IssueDetailsDialog';

const IssueList = ({ projectId }) => {
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedIssueId, setSelectedIssueId] = useState(null);
  const queryClient = useQueryClient();

  // Fetch Issues
  const { data: issues, isLoading, isError } = useQuery({
    queryKey: ['issues', projectId],
    queryFn: async () => {
      const response = await api.get(`/api/issues/project/${projectId}`);
      return response.data;
    },
    enabled: !!projectId,
  });

  // Create Issue Mutation
  const createMutation = useMutation({
    mutationFn: (newIssue) => api.post('/api/issues', { ...newIssue, projectId: Number(projectId) }),
    onSuccess: () => {
      queryClient.invalidateQueries(['issues', projectId]);
      setIsCreateOpen(false);
      toast.success('Issue created successfully');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to create issue');
    }
  });

  // Update Status Mutation
  const statusMutation = useMutation({
    mutationFn: ({ issueId, status }) => api.put(`/api/issues/${issueId}/status/${status}`),
    onSuccess: () => {
      queryClient.invalidateQueries(['issues', projectId]);
      toast.success('Status updated');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to update status');
    }
  });

  // Delete Issue Mutation
  const deleteMutation = useMutation({
    mutationFn: (issueId) => api.delete(`/api/issues/${issueId}`),
    onSuccess: () => {
      queryClient.invalidateQueries(['issues', projectId]);
      toast.success('Issue deleted successfully');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to delete issue');
    }
  });

  const handleCreateSubmit = (data) => {
    createMutation.mutate(data);
  };

  const handleStatusChange = (issueId, newStatus) => {
    statusMutation.mutate({ issueId, status: newStatus });
  };

  const handleDelete = (issueId) => {
    if (window.confirm('Are you sure you want to delete this issue?')) {
      deleteMutation.mutate(issueId);
    }
  };

  const getPriorityColor = (priority) => {
    switch (priority) {
      case 'HIGH': return 'destructive';
      case 'MEDIUM': return 'default';
      case 'LOW': return 'secondary';
      default: return 'outline';
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'DONE': return 'bg-green-500/10 text-green-500 border-green-500/20';
      case 'IN_PROGRESS': return 'bg-blue-500/10 text-blue-500 border-blue-500/20';
      default: return 'bg-yellow-500/10 text-yellow-500 border-yellow-500/20';
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-10 w-full max-w-xs" />
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map(i => <Skeleton key={i} className="h-32 w-full" />)}
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="text-center text-destructive p-8 border rounded-lg border-dashed">
        Failed to load issues.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h3 className="text-2xl font-bold tracking-tight">Issues</h3>
        <Button onClick={() => setIsCreateOpen(true)} className="gap-2">
          <Plus className="h-4 w-4" />
          Create Issue
        </Button>
      </div>

      {(!issues || issues.length === 0) ? (
        <div className="text-center text-muted-foreground p-12 border rounded-lg border-dashed">
          No issues found for this project.
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {issues.map(issue => (
            <Card 
              key={issue.issueId} 
              className="flex flex-col cursor-pointer hover:shadow-md transition-shadow"
              onClick={() => setSelectedIssueId(issue.issueId)}
            >
              <CardHeader className="pb-2">
                <div className="flex items-start justify-between">
                  <Badge variant={getPriorityColor(issue.priority)} className="mb-2">
                    {issue.priority}
                  </Badge>
                  <Button 
                    variant="ghost" 
                    size="icon" 
                    className="h-6 w-6 text-destructive" 
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDelete(issue.issueId);
                    }}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
                <CardTitle className="text-lg leading-tight">{issue.issueTitle}</CardTitle>
              </CardHeader>
              <CardContent className="flex-1 pb-2">
                <p className="text-sm text-muted-foreground line-clamp-2 mb-4">
                  {issue.issueDescription}
                </p>
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <Calendar className="h-3 w-3" />
                  {issue.dueDate ? new Date(issue.dueDate).toLocaleDateString() : 'No due date'}
                </div>
              </CardContent>
              <CardFooter 
                className="pt-2 border-t mt-auto flex items-center justify-between"
                onClick={(e) => e.stopPropagation()}
              >
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="outline" size="sm" className={`h-7 text-xs ${getStatusColor(issue.issueStatus)}`}>
                      {issue.issueStatus?.replace('_', ' ') || 'TODO'}
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent>
                    <DropdownMenuItem onClick={() => handleStatusChange(issue.issueId, 'TODO')}>To Do</DropdownMenuItem>
                    <DropdownMenuItem onClick={() => handleStatusChange(issue.issueId, 'IN_PROGRESS')}>In Progress</DropdownMenuItem>
                    <DropdownMenuItem onClick={() => handleStatusChange(issue.issueId, 'DONE')}>Done</DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>

                {issue.assignedUser && (
                  <div className="text-xs font-medium bg-accent px-2 py-1 rounded-md">
                    {issue.assignedUser.fullName}
                  </div>
                )}
              </CardFooter>
            </Card>
          ))}
        </div>
      )}

      {/* Create Issue Dialog */}
      <IssueForm 
        open={isCreateOpen}
        setOpen={setIsCreateOpen}
        onSubmit={handleCreateSubmit}
        isLoading={createMutation.isPending}
      />

      {/* Issue Details Dialog */}
      <IssueDetailsDialog 
        issueId={selectedIssueId} 
        open={!!selectedIssueId} 
        setOpen={(open) => !open && setSelectedIssueId(null)} 
      />
    </div>
  );
};

export default IssueList;
