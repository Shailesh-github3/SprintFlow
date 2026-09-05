import React, { Suspense } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Briefcase, Tag, AlertCircle } from 'lucide-react';
import api from '../services/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Skeleton } from '../components/ui/skeleton';

const CategoryChart = React.lazy(() => import('../components/CategoryChart'));

const Dashboard = () => {
  const { isAuthenticated } = useAuth();

  // Fetch projects for the dashboard stats
  const { data: projects, isLoading, isError } = useQuery({
    queryKey: ['projects'],
    queryFn: async () => {
      const response = await api.get('/api/projects');
      return response.data.content ?? response.data;
    },
    enabled: isAuthenticated,
  });

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[1, 2, 3, 4].map(i => (
            <Card key={i}><CardHeader><Skeleton className="h-4 w-1/2" /></CardHeader><CardContent><Skeleton className="h-8 w-1/4" /></CardContent></Card>
          ))}
        </div>
        <Skeleton className="h-[300px] w-full rounded-xl" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex h-[400px] items-center justify-center rounded-xl border border-dashed text-destructive">
        <div className="flex flex-col items-center gap-2">
          <AlertCircle className="h-8 w-8" />
          <p>Failed to load dashboard data. Please try again later.</p>
        </div>
      </div>
    );
  }

  // Calculate stats (all derived from the projects query — no placeholders)
  const projectList = Array.isArray(projects) ? projects : [];
  const totalProjects = projectList.length || 0;

  // Process data for chart (Projects by Category)
  const categoryCount = projectList.reduce((acc, project) => {
    const category = project.category || 'Uncategorized';
    acc[category] = (acc[category] || 0) + 1;
    return acc;
  }, {}) || {};

  const totalCategories = Object.keys(categoryCount).length;

  const chartData = Object.keys(categoryCount).map(key => ({
    name: key,
    total: categoryCount[key]
  }));

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Dashboard</h2>
        <p className="text-muted-foreground mt-2">Overview of your projects and tasks.</p>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Projects</CardTitle>
            <Briefcase className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalProjects}</div>
            <p className="text-xs text-muted-foreground">Active projects in your workspace</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Categories</CardTitle>
            <Tag className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalCategories}</div>
            <p className="text-xs text-muted-foreground">
              Distinct project categories in use
            </p>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-7">
        {/* Chart Section */}
        <Card className="col-span-4">
          <CardHeader>
            <CardTitle>Projects by Category</CardTitle>
          </CardHeader>
          <CardContent className="pl-2">
            {chartData.length > 0 ? (
              <Suspense fallback={<Skeleton className="h-[300px] w-full rounded-xl" />}>
                <CategoryChart data={chartData} />
              </Suspense>
            ) : (
              <div className="flex h-[300px] items-center justify-center text-muted-foreground">
                No project data available to display.
              </div>
            )}
          </CardContent>
        </Card>

        {/* Recent Projects List */}
        <Card className="col-span-3">
          <CardHeader>
            <CardTitle>Recent Projects</CardTitle>
            <CardDescription>
              Your most recently created or updated projects.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {projectList.slice(0, 5).map(project => (
                <div key={project.projectId || project.id} className="flex items-center justify-between">
                  <div className="space-y-1">
                    <p className="text-sm font-medium leading-none">
                      <Link to={`/projects/${project.projectId || project.id}`} className="hover:underline">
                        {project.projectName || project.name}
                      </Link>
                    </p>
                    <p className="text-sm text-muted-foreground truncate max-w-[200px]">
                      {project.projectDescription || project.description || 'No description'}
                    </p>
                  </div>
                  <div className="text-xs text-muted-foreground bg-accent px-2 py-1 rounded-md">
                    {project.category || 'Uncategorized'}
                  </div>
                </div>
              ))}
              
              {projectList.length === 0 && (
                <div className="text-sm text-muted-foreground text-center py-4">
                  No projects found.
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default Dashboard;
