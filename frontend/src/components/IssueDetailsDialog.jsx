import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Calendar, Tag } from 'lucide-react';
import api from '../services/api';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from './ui/dialog';
import { Badge } from './ui/badge';
import { Skeleton } from './ui/skeleton';
import CommentList from './CommentList';

const IssueDetailsDialog = ({ issueId, open, setOpen }) => {
  // Fetch Issue details
  const { data: issue, isLoading, isError } = useQuery({
    queryKey: ['issues', 'detail', issueId],
    queryFn: async () => {
      const response = await api.get(`/api/issues/${issueId}`);
      return response.data;
    },
    enabled: !!issueId && open,
  });

  const getPriorityColor = (priority) => {
    switch (priority) {
      case 'HIGH': return 'destructive';
      case 'MEDIUM': return 'default';
      case 'LOW': return 'secondary';
      default: return 'outline';
    }
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent className="sm:max-w-[600px] max-h-[85vh] overflow-y-auto">
        {isLoading ? (
          <div className="space-y-4 py-4">
            <Skeleton className="h-6 w-1/2" />
            <Skeleton className="h-20 w-full" />
          </div>
        ) : isError || !issue ? (
          <div className="text-center text-destructive py-8">
            Failed to load issue details.
          </div>
        ) : (
          <>
            <DialogHeader>
              <div className="flex items-center gap-2 mb-2">
                <Badge variant={getPriorityColor(issue.priority)}>{issue.priority}</Badge>
                <Badge variant="outline">{issue.issueStatus?.replace('_', ' ') || 'TODO'}</Badge>
              </div>
              <DialogTitle className="text-2xl">{issue.issueTitle}</DialogTitle>
              <DialogDescription className="flex items-center gap-2 mt-2">
                <Calendar className="h-3 w-3" />
                {issue.dueDate ? new Date(issue.dueDate).toLocaleDateString() : 'No due date'}
              </DialogDescription>
            </DialogHeader>
            
            <div className="py-4 space-y-6">
              {/* Description */}
              <div>
                <h4 className="text-sm font-semibold mb-2">Description</h4>
                <p className="text-sm text-foreground whitespace-pre-wrap leading-relaxed">
                  {issue.issueDescription || 'No description provided.'}
                </p>
              </div>

              {/* Tags if any */}
              {issue.tags && issue.tags.length > 0 && (
                <div>
                  <h4 className="text-sm font-semibold mb-2">Tags</h4>
                  <div className="flex flex-wrap gap-2">
                    {issue.tags.map((tag, i) => (
                      <Badge variant="secondary" key={i}>
                        <Tag className="h-3 w-3 mr-1" /> {tag}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}

              {/* Assignee */}
              <div>
                <h4 className="text-sm font-semibold mb-1">Assignee</h4>
                <p className="text-sm">
                  {issue.assignedUser ? issue.assignedUser.fullName : 'Unassigned'}
                </p>
              </div>

              <hr className="my-4" />

              {/* Comments Section */}
              <CommentList issueId={issueId} />
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default IssueDetailsDialog;
