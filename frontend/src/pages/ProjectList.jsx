import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Plus, Search, MoreVertical, Edit, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import api from '../services/api';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle, CardFooter } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Skeleton } from '../components/ui/skeleton';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '../components/ui/dropdown-menu';
import ProjectForm from '../components/ProjectForm';
import ConfirmDialog from '../components/ConfirmDialog';
import Pagination from '../components/Pagination';
import { useDebounce } from '../hooks/useDebounce';

const PAGE_SIZE = 12;

const ProjectList = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingProject, setEditingProject] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const queryClient = useQueryClient();
  const debouncedSearch = useDebounce(searchTerm.trim());

  const { isAuthenticated } = useAuth();

  // Server-side search (debounced) + pagination. The backend returns a
  // Spring `Page`; `content` holds the current slice.
  const { data: projectPage, isLoading, isError } = useQuery({
    queryKey: ['projects', page, debouncedSearch],
    queryFn: async () => {
      const params = { page, size: PAGE_SIZE };
      const url = debouncedSearch ? '/api/projects/search' : '/api/projects';
      if (debouncedSearch) params.name = debouncedSearch;
      const response = await api.get(url, { params });
      return response.data;
    },
    enabled: isAuthenticated,
    placeholderData: (previous) => previous,
  });

  const projects = projectPage?.content ?? [];
  const totalPages = projectPage?.totalPages ?? 0;
  const currentPage = projectPage?.number ?? 0;

  // Create Project Mutation
  const createMutation = useMutation({
    mutationFn: (newProject) => api.post('/api/projects', newProject),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      setIsCreateOpen(false);
      setPage(0);
      toast.success('Project created successfully');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to create project');
    }
  });

  // Update Project Mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => api.put(`/api/projects/${id}`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      setEditingProject(null);
      toast.success('Project updated successfully');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to update project');
    }
  });

  // Delete Project Mutation
  const deleteMutation = useMutation({
    mutationFn: (id) => api.delete(`/api/projects/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      setDeleteTarget(null);
      toast.success('Project deleted successfully');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to delete project');
    }
  });

  const handleCreateSubmit = (data) => {
    createMutation.mutate(data);
  };

  const handleUpdateSubmit = (data) => {
    updateMutation.mutate({ id: editingProject.projectId, data });
  };

  const handleDelete = (id) => {
    setDeleteTarget(id);
  };

  const openEditDialog = (project) => {
    setEditingProject({
      ...project,
      tags: project.tags?.join(', ') || ''
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Projects</h2>
          <p className="text-muted-foreground mt-2">Manage your workspaces and initiatives.</p>
        </div>
        <Button onClick={() => setIsCreateOpen(true)} className="gap-2">
          <Plus className="h-4 w-4" />
          New Project
        </Button>
      </div>

      <div className="flex items-center gap-2 max-w-sm">
        <div className="relative flex-1">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input 
            placeholder="Search projects..." 
            className="pl-8" 
            value={searchTerm}
            onChange={(e) => { setSearchTerm(e.target.value); setPage(0); }}
          />
        </div>
      </div>

      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3, 4, 5, 6].map(i => (
            <Card key={i} className="flex flex-col justify-between">
              <CardHeader><Skeleton className="h-6 w-3/4" /></CardHeader>
              <CardContent><Skeleton className="h-16 w-full" /></CardContent>
              <CardFooter><Skeleton className="h-4 w-1/4" /></CardFooter>
            </Card>
          ))}
        </div>
      ) : isError ? (
        <div className="text-center text-destructive p-8 border rounded-lg border-dashed">
          Failed to load projects.
        </div>
      ) : projects.length === 0 ? (
        <div className="text-center text-muted-foreground p-12 border rounded-lg border-dashed">
          {debouncedSearch ? `No projects match "${debouncedSearch}".` : 'No projects found. Create one to get started.'}
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {projects.map((project) => (
            <Card key={project.projectId} className="flex flex-col transition-all hover:shadow-md">
              <CardHeader className="flex flex-row items-start justify-between pb-2">
                <div className="space-y-1 pr-4">
                  <CardTitle className="text-xl">
                    <Link to={`/projects/${project.projectId}`} className="hover:underline">
                      {project.projectName}
                    </Link>
                  </CardTitle>
                  <CardDescription>{project.category}</CardDescription>
                </div>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" className="h-8 w-8 p-0">
                      <span className="sr-only">Open menu</span>
                      <MoreVertical className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => openEditDialog(project)}>
                      <Edit className="mr-2 h-4 w-4" /> Edit
                    </DropdownMenuItem>
                    <DropdownMenuItem className="text-destructive" onClick={() => handleDelete(project.projectId)}>
                      <Trash2 className="mr-2 h-4 w-4" /> Delete
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </CardHeader>
              <CardContent className="flex-1 pb-4">
                <p className="text-sm text-muted-foreground line-clamp-3">
                  {project.projectDescription}
                </p>
                {project.tags && project.tags.length > 0 && (
                  <div className="mt-4 flex flex-wrap gap-2">
                    {project.tags.map((tag, i) => (
                      <Badge variant="secondary" key={i}>{tag}</Badge>
                    ))}
                  </div>
                )}
              </CardContent>
              <CardFooter className="border-t pt-4 text-xs text-muted-foreground flex justify-between">
                <span>Owner: {project.projectOwner?.fullName || 'Unknown'}</span>
                {/* Team size can be added here if available in list DTO */}
              </CardFooter>
            </Card>
          ))}
        </div>
      )}

      <Pagination page={currentPage} totalPages={totalPages} onChange={setPage} />

      {/* Create Dialog */}
      <ProjectForm 
        open={isCreateOpen} 
        setOpen={setIsCreateOpen} 
        onSubmit={handleCreateSubmit} 
        isLoading={createMutation.isPending} 
      />

      {/* Edit Dialog */}
      <ProjectForm 
        open={!!editingProject} 
        setOpen={(open) => !open && setEditingProject(null)} 
        defaultValues={editingProject} 
        onSubmit={handleUpdateSubmit} 
        isLoading={updateMutation.isPending} 
      />

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={!!deleteTarget}
        setOpen={(open) => !open && setDeleteTarget(null)}
        title="Delete project?"
        description="This will permanently delete the project and all its issues, comments, and chat history."
        confirmLabel="Delete project"
        isLoading={deleteMutation.isPending}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget)}
      />
    </div>
  );
};

export default ProjectList;
