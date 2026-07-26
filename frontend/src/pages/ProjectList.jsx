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

const ProjectList = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingProject, setEditingProject] = useState(null);
  
  const queryClient = useQueryClient();

  const { isAuthenticated } = useAuth();

  // Fetch Projects
  const { data: projects, isLoading, isError } = useQuery({
    queryKey: ['projects'],
    queryFn: async () => {
      const response = await api.get('/api/projects');
      return response.data;
    },
    enabled: isAuthenticated,
  });

  // Create Project Mutation
  const createMutation = useMutation({
    mutationFn: (newProject) => api.post('/api/projects', newProject),
    onSuccess: () => {
      queryClient.invalidateQueries(['projects']);
      setIsCreateOpen(false);
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
      queryClient.invalidateQueries(['projects']);
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
      queryClient.invalidateQueries(['projects']);
      toast.success('Project deleted successfully');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to delete project');
    }
  });

  // Filter projects by search term
  const filteredProjects = projects?.filter(p => 
    p.projectName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    p.category?.toLowerCase().includes(searchTerm.toLowerCase())
  ) || [];

  const handleCreateSubmit = (data) => {
    createMutation.mutate(data);
  };

  const handleUpdateSubmit = (data) => {
    updateMutation.mutate({ id: editingProject.projectId, data });
  };

  const handleDelete = (id) => {
    if (window.confirm('Are you sure you want to delete this project?')) {
      deleteMutation.mutate(id);
    }
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
            onChange={(e) => setSearchTerm(e.target.value)}
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
      ) : filteredProjects.length === 0 ? (
        <div className="text-center text-muted-foreground p-12 border rounded-lg border-dashed">
          No projects found. Create one to get started.
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filteredProjects.map((project) => (
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
    </div>
  );
};

export default ProjectList;
