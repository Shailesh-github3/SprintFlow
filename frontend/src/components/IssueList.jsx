import React, { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2, Calendar, LayoutList, Columns3, Search, CheckCircle2, Clock, AlertCircle } from 'lucide-react';
import { toast } from 'sonner';
import api from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { useDebounce } from '../hooks/useDebounce';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Card, CardContent, CardHeader, CardTitle, CardFooter } from './ui/card';
import { Badge } from './ui/badge';
import { Skeleton } from './ui/skeleton';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from './ui/dropdown-menu';
import IssueForm from './IssueForm';
import IssueDetailsDialog from './IssueDetailsDialog';
import ConfirmDialog from './ConfirmDialog';
import Pagination from './Pagination';

const PAGE_SIZE = 12;

const IssueList = ({ projectId, project }) => {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedIssueId, setSelectedIssueId] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [page, setPage] = useState(0);

  // View mode: 'list' or 'kanban'
  const [viewMode, setViewMode] = useState('list');

  // Filter & Search states
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounce(search, 300);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [assigneeFilter, setAssigneeFilter] = useState('ALL');

  const isOwner = Boolean(
    user?.email &&
    project?.projectOwner?.email &&
    user.email.toLowerCase() === project.projectOwner.email.toLowerCase()
  );

  // Eligible members for assignee filter
  const eligibleAssignees = useMemo(() => {
    const map = new Map();
    if (project?.projectOwner?.userId || project?.projectOwner?.id) {
      const id = project.projectOwner.userId || project.projectOwner.id;
      map.set(id, project.projectOwner);
    }
    (project?.projectMembers || []).forEach((m) => {
      const id = m?.userId || m?.id;
      if (id) {
        map.set(id, m);
      }
    });
    return Array.from(map.values());
  }, [project]);

  // Fetch Issues with multi-criteria filters
  const queryParams = {
    page: viewMode === 'kanban' ? 0 : page,
    size: viewMode === 'kanban' ? 100 : PAGE_SIZE,
    status: statusFilter !== 'ALL' ? statusFilter : undefined,
    priority: priorityFilter !== 'ALL' ? priorityFilter : undefined,
    assigneeId: assigneeFilter !== 'ALL' ? Number(assigneeFilter) : undefined,
    search: debouncedSearch.trim() || undefined,
  };

  const { data: issuePage, isLoading, isError } = useQuery({
    queryKey: ['issues', projectId, page, viewMode, statusFilter, priorityFilter, assigneeFilter, debouncedSearch],
    queryFn: async () => {
      const response = await api.get(`/api/issues/project/${projectId}`, { params: queryParams });
      return response.data;
    },
    enabled: !!projectId,
    placeholderData: (previous) => previous,
  });

  const issues = useMemo(() => issuePage?.content ?? [], [issuePage?.content]);
  const totalPages = issuePage?.totalPages ?? 0;
  const currentPage = issuePage?.number ?? 0;

  // Create Issue Mutation
  const createMutation = useMutation({
    mutationFn: (newIssue) => api.post('/api/issues', { ...newIssue, projectId: Number(projectId) }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issues', projectId] });
      queryClient.invalidateQueries({ queryKey: ['activity', projectId] });
      setIsCreateOpen(false);
      setPage(0);
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
      queryClient.invalidateQueries({ queryKey: ['issues', projectId] });
      queryClient.invalidateQueries({ queryKey: ['activity', projectId] });
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
      queryClient.invalidateQueries({ queryKey: ['issues', projectId] });
      queryClient.invalidateQueries({ queryKey: ['activity', projectId] });
      setDeleteTarget(null);
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
    setDeleteTarget(issueId);
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

  // Group issues for Kanban
  const kanbanColumns = useMemo(() => {
    return {
      TODO: issues.filter((i) => (i.issueStatus || 'TODO') === 'TODO'),
      IN_PROGRESS: issues.filter((i) => i.issueStatus === 'IN_PROGRESS'),
      DONE: issues.filter((i) => i.issueStatus === 'DONE'),
    };
  }, [issues]);

  return (
    <div className="space-y-6">
      {/* Top Header & Actions */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 className="text-2xl font-bold tracking-tight">Issues</h3>
          <p className="text-xs text-muted-foreground mt-0.5">Manage, track, and assign project tasks</p>
        </div>

        <div className="flex items-center gap-2">
          {/* View mode toggle */}
          <div className="flex items-center rounded-lg border bg-muted/30 p-1">
            <Button
              variant={viewMode === 'list' ? 'secondary' : 'ghost'}
              size="sm"
              className="h-8 px-2.5 gap-1.5 text-xs font-medium"
              onClick={() => setViewMode('list')}
            >
              <LayoutList className="h-3.5 w-3.5" />
              List
            </Button>
            <Button
              variant={viewMode === 'kanban' ? 'secondary' : 'ghost'}
              size="sm"
              className="h-8 px-2.5 gap-1.5 text-xs font-medium"
              onClick={() => setViewMode('kanban')}
            >
              <Columns3 className="h-3.5 w-3.5" />
              Kanban
            </Button>
          </div>

          <Button onClick={() => setIsCreateOpen(true)} className="gap-2">
            <Plus className="h-4 w-4" />
            Create Issue
          </Button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col gap-3 rounded-lg border bg-card p-3 shadow-sm md:flex-row md:items-center">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search issues by title..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            className="pl-9 h-9 text-sm"
          />
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Status filter */}
          <select
            className="h-9 rounded-md border border-input bg-background px-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
          >
            <option value="ALL">All Statuses</option>
            <option value="TODO">To Do</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="DONE">Done</option>
          </select>

          {/* Priority filter */}
          <select
            className="h-9 rounded-md border border-input bg-background px-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            value={priorityFilter}
            onChange={(e) => {
              setPriorityFilter(e.target.value);
              setPage(0);
            }}
          >
            <option value="ALL">All Priorities</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </select>

          {/* Assignee filter */}
          <select
            className="h-9 rounded-md border border-input bg-background px-2.5 text-xs shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring max-w-[150px]"
            value={assigneeFilter}
            onChange={(e) => {
              setAssigneeFilter(e.target.value);
              setPage(0);
            }}
          >
            <option value="ALL">All Assignees</option>
            {eligibleAssignees.map((m) => {
              const id = m.userId || m.id;
              return (
                <option key={id} value={id}>
                  {m.fullName}
                </option>
              );
            })}
          </select>
        </div>
      </div>

      {isLoading ? (
        <div className="space-y-4">
          <Skeleton className="h-10 w-full max-w-xs" />
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-32 w-full" />
            ))}
          </div>
        </div>
      ) : isError ? (
        <div className="text-center text-destructive p-8 border rounded-lg border-dashed">
          Failed to load issues.
        </div>
      ) : (!issues || issues.length === 0) ? (
        <div className="text-center text-muted-foreground p-12 border rounded-lg border-dashed">
          No issues match your current filters.
        </div>
      ) : viewMode === 'list' ? (
        /* List / Grid View */
        <>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            {issues.map((issue) => {
              const isCreator = user?.email && issue.createdBy?.email && user.email.toLowerCase() === issue.createdBy.email.toLowerCase();
              const canDelete = isOwner || isCreator;

              return (
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
                      {canDelete && (
                        <Button 
                          variant="ghost" 
                          size="icon" 
                          className="h-6 w-6 text-muted-foreground hover:text-destructive" 
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDelete(issue.issueId);
                          }}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                    <CardTitle className="text-lg leading-tight line-clamp-2">{issue.issueTitle}</CardTitle>
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

                    {issue.assignedUser ? (
                      <div className="text-xs font-medium bg-accent px-2 py-1 rounded-md flex items-center gap-1.5" title={`Assigned to ${issue.assignedUser.fullName}`}>
                        <span className="w-1.5 h-1.5 rounded-full bg-primary inline-block"></span>
                        <span className="truncate max-w-[120px]">{issue.assignedUser.fullName}</span>
                      </div>
                    ) : (
                      <span className="text-xs text-muted-foreground italic">Unassigned</span>
                    )}
                  </CardFooter>
                </Card>
              );
            })}
          </div>

          <Pagination page={currentPage} totalPages={totalPages} onChange={setPage} />
        </>
      ) : (
        /* Kanban Board View */
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-start">
          {/* TO DO Column */}
          <div className="rounded-lg border bg-muted/20 p-3 shadow-sm">
            <div className="flex items-center justify-between border-b pb-2 mb-3">
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4 text-yellow-500" />
                <h4 className="font-semibold text-sm">To Do</h4>
              </div>
              <Badge variant="outline" className="text-xs">{kanbanColumns.TODO.length}</Badge>
            </div>
            <div className="space-y-3 min-h-[300px]">
              {kanbanColumns.TODO.map((issue) => (
                <KanbanCard
                  key={issue.issueId}
                  issue={issue}
                  isOwner={isOwner}
                  user={user}
                  onSelect={setSelectedIssueId}
                  onDelete={handleDelete}
                  onStatusChange={handleStatusChange}
                  getPriorityColor={getPriorityColor}
                />
              ))}
              {kanbanColumns.TODO.length === 0 && (
                <div className="py-8 text-center text-xs text-muted-foreground italic border border-dashed rounded-md">
                  No tasks
                </div>
              )}
            </div>
          </div>

          {/* IN PROGRESS Column */}
          <div className="rounded-lg border bg-muted/20 p-3 shadow-sm">
            <div className="flex items-center justify-between border-b pb-2 mb-3">
              <div className="flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-blue-500" />
                <h4 className="font-semibold text-sm">In Progress</h4>
              </div>
              <Badge variant="outline" className="text-xs">{kanbanColumns.IN_PROGRESS.length}</Badge>
            </div>
            <div className="space-y-3 min-h-[300px]">
              {kanbanColumns.IN_PROGRESS.map((issue) => (
                <KanbanCard
                  key={issue.issueId}
                  issue={issue}
                  isOwner={isOwner}
                  user={user}
                  onSelect={setSelectedIssueId}
                  onDelete={handleDelete}
                  onStatusChange={handleStatusChange}
                  getPriorityColor={getPriorityColor}
                />
              ))}
              {kanbanColumns.IN_PROGRESS.length === 0 && (
                <div className="py-8 text-center text-xs text-muted-foreground italic border border-dashed rounded-md">
                  No tasks
                </div>
              )}
            </div>
          </div>

          {/* DONE Column */}
          <div className="rounded-lg border bg-muted/20 p-3 shadow-sm">
            <div className="flex items-center justify-between border-b pb-2 mb-3">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="h-4 w-4 text-green-500" />
                <h4 className="font-semibold text-sm">Done</h4>
              </div>
              <Badge variant="outline" className="text-xs">{kanbanColumns.DONE.length}</Badge>
            </div>
            <div className="space-y-3 min-h-[300px]">
              {kanbanColumns.DONE.map((issue) => (
                <KanbanCard
                  key={issue.issueId}
                  issue={issue}
                  isOwner={isOwner}
                  user={user}
                  onSelect={setSelectedIssueId}
                  onDelete={handleDelete}
                  onStatusChange={handleStatusChange}
                  getPriorityColor={getPriorityColor}
                />
              ))}
              {kanbanColumns.DONE.length === 0 && (
                <div className="py-8 text-center text-xs text-muted-foreground italic border border-dashed rounded-md">
                  No tasks
                </div>
              )}
            </div>
          </div>
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
        project={project}
      />

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={!!deleteTarget}
        setOpen={(open) => !open && setDeleteTarget(null)}
        title="Delete issue?"
        description="This will permanently delete the issue and all its comments."
        confirmLabel="Delete issue"
        isLoading={deleteMutation.isPending}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget)}
      />
    </div>
  );
};

// Sub-component for individual Kanban cards
const KanbanCard = ({ issue, isOwner, user, onSelect, onDelete, onStatusChange, getPriorityColor }) => {
  const isCreator = user?.email && issue.createdBy?.email && user.email.toLowerCase() === issue.createdBy.email.toLowerCase();
  const canDelete = isOwner || isCreator;

  return (
    <Card
      className="p-3 cursor-pointer hover:shadow-md transition-shadow bg-card"
      onClick={() => onSelect(issue.issueId)}
    >
      <div className="flex items-start justify-between gap-1 mb-1.5">
        <Badge variant={getPriorityColor(issue.priority)} className="text-[10px] px-1.5 py-0">
          {issue.priority}
        </Badge>
        {canDelete && (
          <Button
            variant="ghost"
            size="icon"
            className="h-5 w-5 text-muted-foreground hover:text-destructive"
            onClick={(e) => {
              e.stopPropagation();
              onDelete(issue.issueId);
            }}
          >
            <Trash2 className="h-3 w-3" />
          </Button>
        )}
      </div>

      <h5 className="text-xs font-semibold text-foreground line-clamp-2 leading-snug mb-1">
        {issue.issueTitle}
      </h5>

      <p className="text-[11px] text-muted-foreground line-clamp-2 mb-2 leading-tight">
        {issue.issueDescription}
      </p>

      <div
        className="flex items-center justify-between border-t pt-2 text-[10px]"
        onClick={(e) => e.stopPropagation()}
      >
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm" className="h-5 text-[10px] px-1.5 py-0">
              Move
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent>
            <DropdownMenuItem onClick={() => onStatusChange(issue.issueId, 'TODO')}>To Do</DropdownMenuItem>
            <DropdownMenuItem onClick={() => onStatusChange(issue.issueId, 'IN_PROGRESS')}>In Progress</DropdownMenuItem>
            <DropdownMenuItem onClick={() => onStatusChange(issue.issueId, 'DONE')}>Done</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        <span className="text-muted-foreground truncate max-w-[100px]">
          {issue.assignedUser?.fullName || 'Unassigned'}
        </span>
      </div>
    </Card>
  );
};

export default IssueList;
