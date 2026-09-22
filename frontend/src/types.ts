export type UserRole = 'CUSTOMER' | 'PROFESSIONAL' | 'ADMIN';

export interface Address {
  id: string;
  type: 'Home' | 'Work' | 'Other';
  house: string;
  street: string;
  area: string;
  city: string;
  state: string;
  pincode: string;
  landmark?: string;
  isDefault?: boolean;
}

export interface User {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: UserRole;
  avatar?: string;
  addresses: Address[];
  createdAt: string;
}

export interface ServiceVariant {
  id: string;
  name: string;
  price: number;
  originalPrice?: number;
  durationMin: number;
  description: string;
  included: string[];
}

export interface ServiceFAQ {
  question: string;
  answer: string;
}

export interface Service {
  locations: string[];
  id: string;
  slug: string;
  name: string;
  categoryId: string;
  categoryName: string;
  subcategory?: string;
  priceType?: 'FIXED' | 'INSPECTION';
  serviceType?: 'HOME_VISIT' | 'PICKUP_OR_HOME_VISIT';
  warrantyPolicy?: string | null;
  requiredTools?: string[];
  isDemo?: boolean;
  rating: number;
  reviewsCount: number;
  startingPrice: number;
  originalPrice?: number;
  durationMin: number;
  image: string;
  galleryImages?: string[];
  shortDesc: string;
  description: string;
  variants: ServiceVariant[];
  whatIncluded: string[];
  whatExcluded: string[];
  whyChoose: string[];
  steps: string[];
  faqs: ServiceFAQ[];
  popular?: boolean;
  trending?: boolean;
  recommendedReason?: string;
}

export interface Category {
  id: string;
  slug: string;
  name: string;
  icon: string;
  description: string;
  color: string;
  bgPastel: string;
  badge?: string;
  image: string;
  servicesCount: number;
}

export interface Professional {
  id: string;
  name: string;
  phone: string;
  email: string;
  profession: string;
  categoryId: string;
  rating: number;
  reviewsCount: number;
  completedJobs: number;
  experienceYears: number;
  verified: boolean;
  avatar: string;
  bio: string;
  skills: string[];
  certifications: string[];
  languages: string[];
  serviceAreas: string[];
  availableDays: string[];
  workingHours: string;
  isAvailableToday: boolean;
}

export type BookingStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'ASSIGNED'
  | 'ON_THE_WAY'
  | 'ARRIVED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export interface BookingItem {
  service: Service;
  variant: ServiceVariant;
  quantity: number;
}

export interface BookingStatusUpdate {
  status: BookingStatus;
  timestamp: string;
  note: string;
}

export interface Booking {
  id: string;
  bookingNumber?: string;
  customerId?: string;
  customerName?: string;
  customerPhone?: string;
  userId?: string;
  userName?: string;
  userPhone?: string;
  userEmail?: string;
  items?: BookingItem[];
  serviceId?: string;
  serviceName?: string;
  serviceSlug?: string;
  serviceImage?: string;
  variantId?: string;
  variantName?: string;
  professionalId?: string;
  professionalName?: string;
  professionalPhone?: string;
  professionalAvatar?: string;
  professionalRating?: number;
  professionalProfession?: string;
  date?: string;
  timeSlot?: string;
  scheduledDate?: string;
  scheduledTimeSlot?: string;
  address: Address;
  instructions?: string;
  specialInstructions?: string;
  status: BookingStatus;
  statusHistory?: BookingStatusUpdate[];
  subtotal: number;
  discount: number;
  couponApplied?: string;
  couponCode?: string;
  taxes?: number;
  tax?: number;
  total: number;
  paymentMethod: 'UPI' | 'CARD' | 'NETBANKING' | 'WALLET' | 'CASH';
  paymentStatus: 'PAID' | 'PENDING';
  createdAt: string;
  estimatedArrival?: string;
  verificationOtp?: string;
  review?: Review;
}

export interface Review {
  id: string;
  bookingId: string;
  serviceId: string;
  serviceName?: string;
  professionalId?: string;
  professionalName?: string;
  customerName: string;
  customerAvatar?: string;
  customerCity?: string;
  rating: number;
  comment: string;
  tags: string[];
  date: string;
  verifiedBooking: boolean;
}

export interface Coupon {
  code: string;
  discountType: 'FLAT' | 'PERCENTAGE';
  value: number;
  minBookingAmount: number;
  description: string;
  maxDiscount?: number;
  expiry: string;
  // Category ids the coupon is limited to (empty or missing = all services).
  categoryIds?: string[];
  // The discount the server calculated for the current cart. Never computed in the browser.
  discount?: number;
}

export interface NotificationItem {
  id: string;
  title: string;
  message: string;
  type: 'BOOKING' | 'PRO' | 'PAYMENT' | 'OFFER' | 'SYSTEM';
  read: boolean;
  timestamp: string;
  link?: string;
}

export interface SupportTicket {
  id: string;
  ticketNumber: string;
  userId: string;
  userName: string;
  userEmail: string;
  subject: string;
  category: string;
  message: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED';
  createdAt: string;
  replies: {
    id: string;
    sender: string;
    role: 'USER' | 'SUPPORT';
    message: string;
    timestamp: string;
  }[];
}

export interface CartItem {
  service: Service;
  variant: ServiceVariant;
  quantity: number;
}

export type ChatSendBlockedReason =
  | 'CONVERSATION_CLOSED'
  | 'BOOKING_CLOSED'
  | 'BLOCKED_BY_YOU'
  | 'UNAVAILABLE';

// Chat payloads deliberately carry display data only: no user ids, phone numbers or e-mail addresses.
export interface ChatConversation {
  id: string;
  status: 'ACTIVE' | 'CLOSED';
  createdAt: string;
  lastMessageAt: string | null;
  booking: {
    id: string;
    reference: string;
    status: BookingStatus;
    serviceName: string;
    categoryName: string;
    scheduledDate: string;
    scheduledTimeSlot: string;
  };
  counterpart: {
    displayName: string;
    avatar: string | null;
    role: 'CUSTOMER' | 'PROFESSIONAL';
    online: boolean;
  };
  myRole: 'CUSTOMER' | 'PROFESSIONAL';
  canSend: boolean;
  sendBlockedReason: ChatSendBlockedReason | null;
  blockedByMe: boolean;
  unreadCount: number;
  lastMessage: { preview: string | null; deleted: boolean; isMine: boolean; createdAt: string } | null;
}

export type ChatMessageStatus = 'sending' | 'sent' | 'delivered' | 'read' | 'failed';

export interface ChatMessage {
  id: string;
  conversationId: string;
  isMine: boolean;
  type: 'TEXT';
  content: string | null;
  deleted: boolean;
  createdAt: string;
  clientMessageId?: string;
  status?: ChatMessageStatus;
  deliveredAt?: string | null;
  readAt?: string | null;
  // Client-only: why a send failed, and whether resending the same text can succeed.
  failure?: { message: string; retryable: boolean };
}

export type ChatReportReason = 'HARASSMENT' | 'SPAM' | 'OFF_PLATFORM_CONTACT' | 'SAFETY' | 'OTHER';
