// Demo catalog definitions. These are intentionally separate from live bookings and reviews.
// A quoted inspection visit covers diagnosis only; parts and further work need customer approval.
const groups = [
  ['cat-beauty', 'Hair Services', 349, 60, 'hair', `Women's Haircut|Men's Haircut|Kids Haircut|Hair Styling|Hair Spa|Hair Wash & Blow Dry|Hair Coloring|Hair Smoothening|Hair Straightening|Beard Styling|Beard Trimming|Head Massage`],
  ['cat-beauty', 'Manicure & Pedicure', 399, 60, 'nails', `Basic Manicure|Premium Manicure|Gel Manicure|Nail Art|Basic Pedicure|Spa Pedicure|Gel Pedicure|Foot Spa|Cuticle Care`],
  ['cat-beauty', 'Facial & Skincare', 499, 60, 'skincare', `Basic Facial|Glow Facial|Deep Cleansing Facial|Cleanup|De-Tan Treatment|Skin Care Consultation`],
  ['cat-beauty', 'Beauty Services', 299, 45, 'beauty', `Waxing|Full Body Waxing|Eyebrow Threading|Upper Lip Threading|Bridal Makeup|Party Makeup|Saree Draping`],
  ['cat-laptop-computer', 'Laptop Repair', 299, 60, 'laptop', `Laptop Screen Replacement|Laptop Keyboard Replacement|Laptop Battery Replacement|Laptop Charging Port Repair|Laptop Overheating Fix|Laptop Fan Cleaning|Laptop Hinge Repair|Laptop Motherboard Diagnosis|Laptop Speaker Repair|Laptop Webcam Repair|Laptop Touchpad Repair`],
  ['cat-laptop-computer', 'Software Services', 399, 60, 'software', `Windows Installation|Windows Activation Assistance|Driver Installation|Software Installation|Laptop Formatting|Virus & Malware Removal|Laptop Performance Optimization|Data Backup Assistance|Operating System Troubleshooting`],
  ['cat-laptop-computer', 'Hardware & Upgrades', 299, 60, 'laptop', `RAM Upgrade|SSD Upgrade|HDD Replacement|Laptop Cleaning|Desktop Assembly|Computer Repair|Printer Setup|Wi-Fi & Network Troubleshooting`],
  ['cat-electronics', 'Mobile & Tablet', 299, 60, 'mobile', `Mobile Screen Repair|Mobile Battery Replacement|Charging Port Repair|Software Troubleshooting|Tablet Repair`],
  ['cat-electronics', 'Home Electronics', 399, 75, 'electronics', `TV Installation|TV Wall Mounting|TV Repair|Speaker Installation|Home Theatre Setup|CCTV Installation|CCTV Troubleshooting`],
  ['cat-electronics', 'Smart Home', 399, 60, 'smart-home', `Smart Doorbell Installation|Smart Lock Installation|Smart Device Setup|Wi-Fi Router Installation|Smart TV Setup`],
  ['cat-electronics', 'Other Electronics', 299, 60, 'electronics', `Printer Repair|Scanner Setup|Gaming Console Cleaning|Computer Peripheral Setup`],
  ['cat-electrician', 'Electrical Repairs', 299, 60, 'electrical', `Switch & Socket Repair|Ceiling Fan Repair|Inverter Repair|Wiring Repair|Short Circuit Inspection|Geyser Electrical Repair`],
  ['cat-electrician', 'Electrical Installation', 349, 60, 'electrical', `Fan Installation|Light Installation|LED Light Installation|Chandelier Installation|Inverter Installation|MCB Replacement|Doorbell Installation|Exhaust Fan Installation`],
  ['cat-plumber', 'Plumbing Repairs', 299, 60, 'plumbing', `Tap Repair|Wash Basin Repair|Toilet Repair|Water Leakage Repair|Pipe Repair|Drain Cleaning|Sink Blockage Removal`],
  ['cat-plumber', 'Plumbing Installation', 399, 75, 'plumbing', `Tap Installation|Basin Installation|Toilet Installation|Shower Installation|Bathroom Fitting Installation`],
  ['cat-cleaning', 'Home Cleaning', 1499, 180, 'cleaning', `Full Home Cleaning|Kitchen Deep Cleaning|Bathroom Deep Cleaning|Sofa Cleaning|Carpet Cleaning|Mattress Cleaning|Floor Cleaning|Window Cleaning|Water Tank Cleaning|Move-in Cleaning|Move-out Cleaning|Balcony Cleaning`],
  ['cat-ac-appliances', 'AC Services', 499, 75, 'ac', `AC General Servicing|AC Deep Cleaning|AC Gas Refill|AC Installation|AC Uninstallation|AC Repair|AC Cooling Issue|AC Water Leakage Repair`],
  ['cat-ac-appliances', 'Appliances', 399, 75, 'appliances', `Washing Machine Repair|Refrigerator Repair|Microwave Repair|Dishwasher Repair|Geyser Repair|Chimney Cleaning|RO Water Purifier Service|RO Installation|RO Repair`],
];

const inspection = /repair|replacement|refill|issue|diagnosis|troubleshooting|installation|upgrade|assembly|formatting|data backup|water leakage|drain cleaning|blockage/i;
const slugify = (name) => name.toLowerCase().replace(/&/g, 'and').replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
const safety = (category) => category === 'cat-electrician'
  ? 'Work must be performed by a qualified electrician with the circuit isolated before inspection.'
  : '';
const serviceDetails = {
  'Hair Services': 'A stylist confirms the requested look, prepares the work area and completes the selected hair or beard treatment.',
  'Manicure & Pedicure': 'A nail technician prepares and treats the hands or feet using the method selected, then checks the finish with you.',
  'Facial & Skincare': 'A skincare professional discusses skin concerns before the selected cleansing or treatment steps.',
  'Beauty Services': 'A beauty professional confirms the area, style or look requested before beginning the selected treatment.',
  'Laptop Repair': 'A laptop technician checks the reported fault, tests the affected component and explains the repair options.',
  'Software Services': 'A computer technician checks the software issue, confirms data and license requirements, then performs the agreed setup or troubleshooting.',
  'Hardware & Upgrades': 'A computer technician checks compatibility and device condition before fitting, cleaning or configuring hardware.',
  'Mobile & Tablet': 'An electronics technician inspects the device and confirms the affected component before proposing repair work.',
  'Home Electronics': 'An electronics technician checks the equipment, installation point and power or signal connections before the agreed work.',
  'Smart Home': 'A technician checks device compatibility, network access and placement before setup and pairing.',
  'Other Electronics': 'A technician inspects the device, connections and fault symptoms before repair or setup.',
  'Electrical Repairs': 'A qualified electrician isolates the circuit, checks the reported fault and explains the safe repair scope.',
  'Electrical Installation': 'A qualified electrician checks the supply, mounting location and load before installing or replacing the fitting.',
  'Plumbing Repairs': 'A plumber inspects the affected fixture or line, identifies the leak or blockage and explains the repair scope.',
  'Plumbing Installation': 'A plumber checks fixture dimensions, water connections and drainage before installing the selected fitting.',
  'Home Cleaning': 'A cleaning professional checks the space and surfaces, then works through the selected cleaning area and confirms completion.',
  'AC Services': 'An AC technician checks the unit, access and cooling symptoms before the agreed service or inspection.',
  Appliances: 'An appliance technician checks the unit, fault symptoms and safe access before the agreed service or inspection.',
};
const toolsBySubcategory = {
  'Laptop Repair': ['Diagnostic toolkit', 'Precision screwdrivers'],
  'Hardware & Upgrades': ['Diagnostic toolkit', 'Precision screwdrivers'],
  'Software Services': ['Diagnostic laptop', 'Authorized installation media where required'],
  'Electrical Repairs': ['Insulated tools', 'Voltage tester'],
  'Electrical Installation': ['Insulated tools', 'Voltage tester'],
  'Plumbing Repairs': ['Plumbing toolkit'],
  'Plumbing Installation': ['Plumbing toolkit'],
  'AC Services': ['AC service toolkit'],
};

export const EXPANDED_SERVICES = groups.flatMap(([categoryId, subcategory, basePrice, durationMin, imageKey, names]) =>
  names.split('|').map((name) => {
    const slug = slugify(name);
    const isInspection = inspection.test(name);
    const priceType = isInspection ? 'INSPECTION' : 'FIXED';
    const feeNote = isInspection ? 'The listed price covers an inspection visit. Parts and additional work are quoted after diagnosis and require approval.' : 'The listed price covers the standard package. Any optional additions require approval.';
    const safetyNote = safety(categoryId);
    const shortDesc = `${name} at your doorstep with a clear scope and upfront visit price.`;
    const licenseNote = name === 'Windows Activation Assistance' ? 'A valid Windows license is required; license purchase is not included.' : '';
    const description = `${shortDesc} ${serviceDetails[subcategory]} ${feeNote}${safetyNote ? ` ${safetyNote}` : ''}${licenseNote ? ` ${licenseNote}` : ''}`;
    return {
      id: `srv-catalog-${categoryId.slice(4)}-${slug}`,
      slug: `${slug}-${categoryId.slice(4)}`,
      name, categoryId, subcategory, priceType,
      serviceType: categoryId === 'cat-laptop-computer' || categoryId === 'cat-electronics' ? 'PICKUP_OR_HOME_VISIT' : 'HOME_VISIT',
      warrantyPolicy: null,
      requiredTools: toolsBySubcategory[subcategory] || [],
      isDemo: true,
      startingPrice: basePrice,
      durationMin,
      image: `/service-images/${imageKey}.svg`,
      shortDesc, description,
      whatIncluded: isInspection ? ['On-site diagnosis', 'Itemized quote before additional work'] : ['Standard service visit', 'Scope confirmation before work'],
      whatExcluded: [...(isInspection ? ['Replacement parts', 'Repair work beyond diagnosis'] : ['Parts and optional add-ons']), ...(licenseNote ? ['Windows license purchase'] : [])],
      whyChoose: ['Upfront visit price', 'Qualified professional assignment'],
      faqs: [{ question: 'Are parts included?', answer: isInspection ? 'No. Any parts or extra work are quoted after inspection.' : 'Only items listed in the selected package are included.' }],
      variants: [{
        id: `var-catalog-${categoryId.slice(4)}-${slug}`,
        name: isInspection ? 'Inspection visit' : 'Standard service',
        price: basePrice, durationMin,
        description: feeNote,
        included: isInspection ? ['Diagnosis and itemized quote'] : ['Standard service visit'],
      }],
    };
  }),
);

export const EXPANDED_PROFESSIONALS = [
  { id: 'pro-demo-nails', name: 'Neha Singh', profession: 'Nail Technician', categoryId: 'cat-beauty', specialtySubcategories: ['Manicure & Pedicure'], experienceYears: 5, bio: 'Demo nail technician for manicure, pedicure and nail care requests.' },
  { id: 'pro-demo-computers', name: 'Arjun Mehta', profession: 'Laptop Technician', categoryId: 'cat-laptop-computer', experienceYears: 6, bio: 'Demo laptop technician for computer repair, upgrades and software assistance.' },
  { id: 'pro-demo-electronics', name: 'Rohan Kapoor', profession: 'Electronics Technician', categoryId: 'cat-electronics', experienceYears: 6, bio: 'Demo electronics technician for mobile, TV, CCTV and smart home assistance.' },
  { id: 'pro-demo-appliances', name: 'Sanjay Verma', profession: 'Appliance Technician', categoryId: 'cat-ac-appliances', specialtySubcategories: ['Appliances'], experienceYears: 7, bio: 'Demo appliance technician for washing machines, refrigerators and home appliances.' },
];
