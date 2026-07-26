import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import { Label } from './ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from './ui/dialog';

const projectSchema = z.object({
  projectName: z.string().min(3, { message: 'Project name must be at least 3 characters' }),
  projectDescription: z.string().min(10, { message: 'Description must be at least 10 characters' }),
  category: z.string().min(2, { message: 'Category is required' }),
  tags: z.string().optional(),
});

const ProjectForm = ({ open, setOpen, defaultValues, onSubmit, isLoading }) => {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(projectSchema),
    defaultValues: defaultValues || {
      projectName: '',
      projectDescription: '',
      category: '',
      tags: '',
    },
  });

  // Reset form when opened with new defaults
  React.useEffect(() => {
    if (open) {
      reset(defaultValues || {
        projectName: '',
        projectDescription: '',
        category: '',
        tags: '',
      });
    }
  }, [open, defaultValues, reset]);

  const handleFormSubmit = (data) => {
    // Convert comma separated string to array
    const tagsArray = data.tags 
      ? data.tags.split(',').map(tag => tag.trim()).filter(Boolean)
      : [];
    
    onSubmit({
      ...data,
      tags: tagsArray
    });
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>{defaultValues ? 'Update Project' : 'Create Project'}</DialogTitle>
          <DialogDescription>
            {defaultValues 
              ? 'Update your project details below.' 
              : 'Add a new project to your workspace.'}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="projectName">Project Name</Label>
            <Input id="projectName" placeholder="SprintFlow Frontend" {...register('projectName')} disabled={isLoading} />
            {errors.projectName && <p className="text-sm text-destructive">{errors.projectName.message}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="projectDescription">Description</Label>
            <Textarea 
              id="projectDescription" 
              placeholder="A comprehensive frontend for..." 
              {...register('projectDescription')} 
              disabled={isLoading} 
              className="resize-none"
            />
            {errors.projectDescription && <p className="text-sm text-destructive">{errors.projectDescription.message}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="category">Category</Label>
            <Input id="category" placeholder="Web Development" {...register('category')} disabled={isLoading} />
            {errors.category && <p className="text-sm text-destructive">{errors.category.message}</p>}
          </div>

          <div className="space-y-2">
            <Label htmlFor="tags">Tags (comma separated)</Label>
            <Input id="tags" placeholder="react, vite, tailwind" {...register('tags')} disabled={isLoading} />
            <p className="text-xs text-muted-foreground">Optional. E.g: frontend, design</p>
          </div>

          <div className="pt-2 flex justify-end gap-2">
            <Button variant="outline" type="button" onClick={() => setOpen(false)} disabled={isLoading}>
              Cancel
            </Button>
            <Button type="submit" disabled={isLoading}>
              {isLoading ? 'Saving...' : 'Save Project'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};

export default ProjectForm;
