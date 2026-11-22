import React, { Suspense } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Briefcase, AlertCircle, CheckCircle2, Clock, AlertTriangle, Layers, Percent } from 'lucide-react';
import api from '../services/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Skeleton } from '../components/ui/skeleton';

const CategoryChart = React.lazy(() => import('../components/CategoryChart'));

const Dashboard = () => {
  const { isAuthenticated } = useAuth();

  // Fetch projects for recent projects list and category chart
  const { data: projects, isLoading: isProjectsLoading } = useQuery({
    queryKey: ['projects'],
    queryFn: async () => {
      const response = await api.get('/api/projects');
      return response.data.content ?? response.data;
    },
    enabled: isAuthenticated,
  });

  // Fetch aggregated dashboard metrics from the new backend endpoint
  const { data: stats, isLoading: isStatsLoading, isError: isStatsError } = useQuery({
    queryKey: ['users', 'dashboard-stats'],
    queryFn: async () => {
      const response = await api.get('/api/users/dashboard-stats');
      return response.data;
    },
    enabled: isAuthenticated,
  });

  const isLoading = isProjectsLoading || isStatsLoading;

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {[1, 2, 3, 4].map(i => (
            <Card key={i}><CardHeader><Skeleton className="h-4 w-1/2" /></CardHeader><CardContent><Skeleton className="h-8 w-1/4" /></CardContent></Card>
          ))}
        </div>
        <Skeleton className="h-[300px] w-full rounded-xl" />
      </div>
    );
  }

  if (isStatsError) {
    return (
      <div className="flex h-[400px] items-center justify-center rounded-xl border border-dashed text-destructive">
        <div className="flex flex-col items-center gap-2">
          <AlertCircle className="h-8 w-8" />
          <p>Failed to load dashboard data. Please try again later.</p>
        </div>
      </div>
    );
  }

  const projectList = Array.isArray(projects) ? projects : [];

  // Process data for category chart
  const categoryCount = projectList.reduce((acc, project) => {
    const category = project.category || 'Uncategorized';
    acc[category] = (acc[category] || 0) + 1;
    return acc;
  }, {}) || {};

  const chartData = Object.keys(categoryCount).map(key => ({
    name: key,
    total: categoryCount[key]
  }));

  const overdueCount = stats?.overdueIssues ?? 0;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Dashboard</h2>
        <p className="text-muted-foreground mt-1 text-sm">Aggregated metrics, health status, and task progress overview.</p>
      </div>

      {/* Aggregate Metric Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-6">
        {/* Total Projects */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Total Projects</CardTitle>
            <Briefcase className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.totalProjects ?? projectList.length}</div>
            <p className="text-[11px] text-muted-foreground mt-1">Active workspaces</p>
          </CardContent>
        </Card>

        {/* Total Issues */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Total Tasks</CardTitle>
            <Layers className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.totalIssues ?? 0}</div>
            <p className="text-[11px] text-muted-foreground mt-1">Across all projects</p>
          </CardContent>
        </Card>

        {/* In Progress */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">In Progress</CardTitle>
            <Clock className="h-4 w-4 text-blue-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.inProgressIssues ?? 0}</div>
            <p className="text-[11px] text-muted-foreground mt-1">Under development</p>
          </CardContent>
        </Card>

        {/* Completed */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Done</CardTitle>
            <CheckCircle2 className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.completedIssues ?? 0}</div>
            <p className="text-[11px] text-muted-foreground mt-1">Tasks resolved</p>
          </CardContent>
        </Card>

        {/* Overdue Tasks */}
        <Card className={overdueCount > 0 ? "border-destructive/40 bg-destructive/5" : ""}>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Overdue</CardTitle>
            <AlertTriangle className={`h-4 w-4 ${overdueCount > 0 ? "text-destructive" : "text-muted-foreground"}`} />
          </CardHeader>
          <CardContent>
            <div className={`text-2xl font-bold ${overdueCount > 0 ? "text-destructive" : ""}`}>{overdueCount}</div>
            <p className="text-[11px] text-muted-foreground mt-1">Needs attention</p>
          </CardContent>
        </Card>

        {/* Completion Rate */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-xs font-medium text-muted-foreground">Completion Rate</CardTitle>
            <Percent className="h-4 w-4 text-primary" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.completionPercentage ?? 0}%</div>
            <p className="text-[11px] text-muted-foreground mt-1">Average velocity</p>
          </CardContent>
        </Card>
      </div>

      {/* Progress Bar Card */}
      {stats && stats.totalIssues > 0 && (
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-sm font-semibold">Workspace Delivery Progress</CardTitle>
            <CardDescription className="text-xs">
              {stats.completedIssues} of {stats.totalIssues} tasks completed across your projects
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="w-full bg-muted rounded-full h-3 overflow-hidden">
              <div
                className="bg-primary h-3 rounded-full transition-all duration-500"
                style={{ width: `${Math.min(100, Math.max(0, stats.completionPercentage))}%` }}
              ></div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Charts and Recent Projects Grid */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-7">
        {/* Chart Section */}
        <Card className="col-span-4">
          <CardHeader>
            <CardTitle>Projects by Category</CardTitle>
            <CardDescription>Breakdown of workspace projects by domain</CardDescription>
          </CardHeader>
          <CardContent className="pl-2">
            {chartData.length > 0 ? (
              <Suspense fallback={<Skeleton className="h-[300px] w-full rounded-xl" />}>
                <CategoryChart data={chartData} />
              </Suspense>
            ) : (
              <div className="flex h-[300px] items-center justify-center text-muted-foreground text-sm">
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
                    <p className="text-xs text-muted-foreground truncate max-w-[200px]">
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
