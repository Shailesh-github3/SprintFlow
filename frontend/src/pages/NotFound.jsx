import React from 'react';
import { Link } from 'react-router-dom';
import { FileQuestion } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardDescription, CardFooter, CardHeader, CardTitle } from '../components/ui/card';

const NotFound = () => {
  return (
    <div className="flex h-[60vh] items-center justify-center p-4">
      <Card className="w-full max-w-md text-center">
        <CardHeader>
          <div className="flex justify-center mb-4">
            <FileQuestion className="h-12 w-12 text-muted-foreground" />
          </div>
          <CardTitle className="text-2xl">Page not found</CardTitle>
          <CardDescription>
            The page you are looking for does not exist or was moved.
          </CardDescription>
        </CardHeader>
        <CardFooter>
          <Button className="w-full" asChild>
            <Link to="/">Back to Dashboard</Link>
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
};

export default NotFound;
