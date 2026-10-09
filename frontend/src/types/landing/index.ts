export interface ServiceCategory {
  id: string;
  name: string;
  description: string;
  startingPrice: number;
  iconName: string;
  badge?: string;
  popular?: boolean;
}

export interface ServiceItem {
  id: string;
  categoryId: string;
  title: string;
  shortDesc: string;
  price: number;
  originalPrice: number;
  rating: number;
  reviewsCount: number;
  durationMinutes: number;
  warrantyDays: number;
  features: string[];
  popular?: boolean;
  imageUrl?: string;
  discountPercent?: number;
}

export interface TopPro {
  id: string;
  name: string;
  specialty: string;
  experienceYears: number;
  rating: number;
  jobsCompleted: number;
  badge: string;
  city: string;
  quote: string;
  skills: string[];
}

export interface ReviewItem {
  id: string;
  author: string;
  city: string;
  serviceTitle: string;
  rating: number;
  date: string;
  comment: string;
  verified: boolean;
}

export interface FaqItem {
  question: string;
  answer: string;
  category?: string;
}

export interface CartItem {
  service: ServiceItem;
  quantity: number;
}

export interface DiagnosticResult {
  category: string;
  serviceId?: string;
  serviceTitle?: string;
  issueDetected?: string;
  probableCause?: string;
  urgency?: string;
  estimatedPrice?: string;
  estimatedDuration?: string;
  recommendedPackage?: string;
  proSkillRequired?: string;
  guarantee?: string;
  steps?: string[];
  actionSteps?: string[];
}

export interface BookingState {
  id: string;
  otp: string;
  items: CartItem[];
  total: number;
  discount: number;
  couponApplied?: string;
  date: string;
  timeSlot: string;
  address: {
    name: string;
    phone: string;
    flat: string;
    street: string;
    city: string;
    pincode: string;
  };
  paymentMethod: 'cash' | 'upi' | 'card';
  status: 'confirmed' | 'dispatched' | 'in_progress' | 'completed';
  etaMinutes: number;
  pro: {
    name: string;
    phone: string;
    rating: number;
    jobs: number;
    badge: string;
  };
}

export interface UserProfile {
  name: string;
  phone: string;
  isLoggedIn: boolean;
}
