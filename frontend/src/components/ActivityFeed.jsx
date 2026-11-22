import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Activity, PlusCircle, UserCheck, UserPlus, Mail, Clock, AlertCircle } from 'lucide-react';
import api from '../services/api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from './ui/card';
import { Badge } from './ui/badge';
import { Skeleton } from './ui/skeleton';
import { Avatar, AvatarFallback } from './ui/avatar';
import Pagination from './Pagination';

const PAGE_SIZE = 15;

const getActionIcon = (action) => {
  switch (action) {
    case 'CREATED':
      return <PlusCircle className="h-4 w-4 text-emerald-500" />;
    case 'ASSIGNED':
      return <UserCheck className="h-4 w-4 text-blue-500" />;
    case 'STATUS_CHANGED':
      return <Activity className="h-4 w-4 text-amber-500" />;
    case 'INVITED':
      return <Mail className="h-4 w-4 text-purple-500" />;
    case 'JOINED':
      return <UserPlus className="h-4 w-4 text-emerald-500" />;
    default:
      return <Clock className="h-4 w-4 text-muted-foreground" />;
  }
};

const getActionVariant = (action) => {
  switch (action) {
    case 'CREATED':
    case 'JOINED':
      return 'default';
    case 'ASSIGNED':
      return 'secondary';
    case 'STATUS_CHANGED':
      return 'outline';
    default:
      return 'secondary';
  }
};

const ActivityFeed = ({ projectId }) => {
  const [page, setPage] = useState(0);

  const { data: activityPage, isLoading, isError } = useQuery({
    queryKey: ['activity', projectId, page],
    queryFn: async () => {
      const response = await api.get(`/api/projects/${projectId}/activity`, {
        params: { page, size: PAGE_SIZE },
      });
      return response.data;
    },
    enabled: !!projectId,
    placeholderData: (previous) => previous,
  });

  const logs = activityPage?.content ?? [];
  const totalPages = activityPage?.totalPages ?? 0;
  const currentPage = activityPage?.number ?? 0;

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-1/4" />
          <Skeleton className="h-4 w-1/3" />
        </CardHeader>
        <CardContent className="space-y-4">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="flex items-start gap-3 p-3 border rounded-lg">
              <Skeleton className="h-8 w-8 rounded-full" />
              <div className="space-y-2 flex-1">
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-1/4" />
              </div>
            </div>
          ))}
        </CardContent>
      </Card>
    );
  }

  if (isError) {
    return (
      <div className="text-center text-destructive p-8 border rounded-lg border-dashed">
        <AlertCircle className="h-8 w-8 mx-auto mb-2" />
        Failed to load activity feed.
      </div>
    );
  }

  return (
    <Card className="max-w-4xl mx-auto">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Activity className="h-5 w-5" />
          Project Activity
        </CardTitle>
        <CardDescription>
          Append-only audit trail of member and issue events within this project.
        </CardDescription>
      </CardHeader>

      <CardContent className="space-y-4">
        {logs.length === 0 ? (
          <div className="text-center text-muted-foreground py-12">
            <Activity className="h-10 w-10 mx-auto mb-3 opacity-20" />
            <p className="text-sm font-medium">No activity recorded yet.</p>
            <p className="text-xs text-muted-foreground mt-1">
              Events such as issue creation, assignment, and status updates will appear here.
            </p>
          </div>
        ) : (
          <div className="relative pl-6 space-y-4 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-border">
            {logs.map((log) => {
              const initial = log.actorName ? log.actorName.charAt(0).toUpperCase() : 'U';
              const formattedDate = log.createdAt
                ? new Date(log.createdAt).toLocaleString(undefined, {
                    month: 'short',
                    day: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  })
                : 'Just now';

              return (
                <div
                  key={log.auditId}
                  className="relative flex items-start gap-3 p-3 rounded-lg border bg-card hover:bg-accent/40 transition-colors"
                >
                  <div className="absolute -left-[29px] top-4 bg-background border rounded-full p-1 shadow-sm">
                    {getActionIcon(log.action)}
                  </div>

                  <Avatar className="h-8 w-8 mt-0.5">
                    <AvatarFallback className="text-xs font-semibold">
                      {initial}
                    </AvatarFallback>
                  </Avatar>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-2 flex-wrap mb-1">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-medium leading-none">
                          {log.actorName || log.actorEmail}
                        </span>
                        <Badge variant={getActionVariant(log.action)} className="text-[10px] px-1.5 py-0">
                          {log.action}
                        </Badge>
                      </div>
                      <span className="text-xs text-muted-foreground">
                        {formattedDate}
                      </span>
                    </div>

                    <p className="text-sm text-foreground/90 leading-relaxed whitespace-pre-wrap">
                      {log.summary || `${log.action} on ${log.entityType?.toLowerCase() || 'item'}`}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        <Pagination page={currentPage} totalPages={totalPages} onChange={setPage} />
      </CardContent>
    </Card>
  );
};

export default ActivityFeed;
