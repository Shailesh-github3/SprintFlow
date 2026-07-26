import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Send, MessageSquare } from 'lucide-react';
import { toast } from 'sonner';
import api from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Skeleton } from './ui/skeleton';
import { Avatar, AvatarFallback } from './ui/avatar';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';

const ProjectChat = ({ projectId }) => {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [newMessage, setNewMessage] = useState('');
  const messagesEndRef = useRef(null);

  // Fetch Chat Messages
  const { data: messages, isLoading, isError } = useQuery({
    queryKey: ['chat', projectId],
    queryFn: async () => {
      const response = await api.get(`/api/messages/chat/${projectId}`);
      return response.data;
    },
    enabled: !!projectId,
    // Poll every 5 seconds for new messages since we don't have WebSockets in this prototype
    refetchInterval: 5000, 
  });

  // Send Message Mutation
  const sendMutation = useMutation({
    mutationFn: (content) => api.post('/api/messages/send', { projectId: Number(projectId), content }),
    onSuccess: () => {
      queryClient.invalidateQueries(['chat', projectId]);
      setNewMessage('');
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || 'Failed to send message');
    }
  });

  const handleSend = (e) => {
    e.preventDefault();
    if (!newMessage.trim()) return;
    sendMutation.mutate(newMessage);
  };

  // Auto-scroll to bottom when messages update
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages]);

  return (
    <Card className="flex flex-col h-[500px]">
      <CardHeader className="border-b py-4">
        <CardTitle className="flex items-center gap-2 text-lg">
          <MessageSquare className="h-5 w-5" />
          Project Chat
        </CardTitle>
      </CardHeader>
      
      <CardContent className="flex-1 flex flex-col p-0 overflow-hidden">
        {/* Messages Area */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {isLoading ? (
            <div className="space-y-4">
              {[1, 2, 3].map(i => (
                <div key={i} className={`flex ${i % 2 === 0 ? 'justify-end' : 'justify-start'}`}>
                  <Skeleton className="h-12 w-2/3 rounded-lg" />
                </div>
              ))}
            </div>
          ) : isError ? (
            <div className="flex h-full items-center justify-center text-sm text-destructive">
              Failed to load chat history.
            </div>
          ) : messages?.length === 0 ? (
            <div className="flex h-full flex-col items-center justify-center text-muted-foreground space-y-2">
              <MessageSquare className="h-8 w-8 opacity-20" />
              <p className="text-sm">No messages yet. Say hello!</p>
            </div>
          ) : (
            messages?.map((msg) => {
              const isMine = msg.sender?.email === user?.email;
              return (
                <div key={msg.messageId} className={`flex flex-col ${isMine ? 'items-end' : 'items-start'}`}>
                  <div className="flex items-end gap-2 max-w-[80%]">
                    {!isMine && (
                      <Avatar className="h-6 w-6 mb-1">
                        <AvatarFallback className="text-[10px]">
                          {msg.sender?.fullName?.charAt(0) || 'U'}
                        </AvatarFallback>
                      </Avatar>
                    )}
                    <div className={`flex flex-col ${isMine ? 'items-end' : 'items-start'}`}>
                      {!isMine && <span className="text-[10px] text-muted-foreground mb-1 ml-1">{msg.sender?.fullName}</span>}
                      <div 
                        className={`rounded-2xl px-4 py-2 text-sm ${
                          isMine 
                            ? 'bg-primary text-primary-foreground rounded-br-sm' 
                            : 'bg-muted text-foreground rounded-bl-sm'
                        }`}
                      >
                        {msg.messageText}
                      </div>
                      <span className="text-[10px] text-muted-foreground mt-1 opacity-70">
                        {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Input Area */}
        <div className="p-4 border-t bg-card">
          <form onSubmit={handleSend} className="flex gap-2">
            <Input 
              placeholder="Type a message..." 
              value={newMessage}
              onChange={(e) => setNewMessage(e.target.value)}
              disabled={sendMutation.isPending}
              className="flex-1"
            />
            <Button type="submit" size="icon" disabled={sendMutation.isPending || !newMessage.trim()}>
              <Send className="h-4 w-4" />
              <span className="sr-only">Send</span>
            </Button>
          </form>
        </div>
      </CardContent>
    </Card>
  );
};

export default ProjectChat;
