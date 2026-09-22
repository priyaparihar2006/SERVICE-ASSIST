ALTER TABLE "Service" ADD COLUMN "galleryImages" TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[];

UPDATE "Service"
SET "image" = '/service-images/ro-purifier/servicing.png',
    "galleryImages" = ARRAY[
      '/service-images/ro-purifier/inspection-water-test.png',
      '/service-images/ro-purifier/filter-replacement.png',
      '/service-images/ro-purifier/installation.png',
      '/service-images/ro-purifier/maintenance-cartridges.png'
    ]::TEXT[]
WHERE "id" = 'srv-water-purifier-ro'
  AND "name" = 'RO Water Purifier Service & Filter Replacement'
  AND "image" = 'https://images.unsplash.com/photo-1548839140-29a749e1bc4e?auto=format&fit=crop&w=800&q=80';

UPDATE "Category"
SET "image" = '/service-images/ro-purifier/servicing.png'
WHERE "id" = 'cat-water-purifier'
  AND "image" = 'https://images.unsplash.com/photo-1548839140-29a749e1bc4e?auto=format&fit=crop&w=800&q=80';
