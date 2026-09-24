-- Change only records that still use the inspected image reference. Custom edits remain untouched.
UPDATE "Service" SET "image" = '/service-images/srv-ac-foamjet.png'
WHERE "id" = 'srv-ac-foamjet'
  AND "image" = 'https://images.unsplash.com/photo-1621905251189-08b45d6a269e?auto=format&fit=crop&w=800&q=80';

UPDATE "Service" SET "image" = '/service-images/srv-bathroom-deep.png'
WHERE "id" = 'srv-bathroom-deep'
  AND "image" = 'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=800&q=80';

UPDATE "Service" SET "image" = '/service-images/srv-catalog-ac-appliances-ac-cooling-issue.png'
WHERE "id" = 'srv-catalog-ac-appliances-ac-cooling-issue'
  AND "image" = '/service-images/ac.svg';
