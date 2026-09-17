import { User, Category, Service, Professional, Booking, Review, Coupon, NotificationItem, SupportTicket, BookingStatus, Address } from '../src/types';
import { CATEGORIES, SERVICES, PROFESSIONALS, COUPONS, REVIEWS, DEMO_USERS, CITIES } from './seedData';

class DatabaseStore {
  public users: User[] = [...DEMO_USERS];
  public categories: Category[] = [...CATEGORIES];
  public services: Service[] = [...SERVICES];
  public professionals: Professional[] = [...PROFESSIONALS];
  public coupons: Coupon[] = [...COUPONS];
  public reviews: Review[] = [...REVIEWS];
  public bookings: Booking[] = [];
  public notifications: NotificationItem[] = [];
  public tickets: SupportTicket[] = [];

  constructor() {
    this.seedInitialBookings();
    this.seedInitialNotifications();
    this.seedInitialTickets();
  }

  private seedInitialBookings() {
    const defaultAddr: Address = DEMO_USERS[0].addresses[0];
    this.bookings = [
      {
        id: 'bk-1001',
        bookingNumber: 'SRV-2026-8941',
        customerId: 'usr-customer-1',
        customerName: 'Priya Sharma',
        customerPhone: '+91 98765 12345',
        serviceId: 'srv-ac-foamjet',
        serviceName: 'Foam Jet Power AC Servicing',
        serviceSlug: 'ac-jet-service',
        serviceImage: 'https://images.unsplash.com/photo-1621905251189-08b45d6a269e?auto=format&fit=crop&w=800&q=80',
        variantId: 'var-split-1',
        variantName: 'Split AC Power Jet Clean (1 Unit)',
        professionalId: 'pro-rahul',
        professionalName: 'Rahul Sharma',
        professionalPhone: '+91 98765 43210',
        professionalAvatar: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=300&q=80',
        professionalRating: 4.9,
        professionalProfession: 'Senior HVAC & AC Specialist',
        date: '2026-09-17',
        timeSlot: '11:00 AM - 12:00 PM',
        address: defaultAddr,
        instructions: 'Please call before ringing the doorbell, baby sleeping.',
        status: 'ON_THE_WAY',
        statusHistory: [
          { status: 'CONFIRMED', timestamp: '2026-09-16 09:30 AM', note: 'Booking confirmed & slot reserved' },
          { status: 'ASSIGNED', timestamp: '2026-09-16 10:15 AM', note: 'Rahul Sharma assigned as your AC Specialist' },
          { status: 'ON_THE_WAY', timestamp: '2026-09-16 10:45 AM', note: 'Professional is on the way (ETA: 15 mins)' },
        ],
        subtotal: 599,
        discount: 150,
        couponApplied: 'WELCOME150',
        taxes: 22,
        total: 471,
        paymentMethod: 'UPI',
        paymentStatus: 'PAID',
        createdAt: '2026-09-16 09:30 AM',
        estimatedArrival: '15 mins',
      },
      {
        id: 'bk-1002',
        bookingNumber: 'SRV-2026-7729',
        customerId: 'usr-customer-1',
        customerName: 'Priya Sharma',
        customerPhone: '+91 98765 12345',
        serviceId: 'srv-bathroom-deep',
        serviceName: 'Intense Bathroom Deep Cleansing',
        serviceSlug: 'bathroom-deep-cleaning',
        serviceImage: 'https://images.unsplash.com/photo-1584622650111-993a426fbf0a?auto=format&fit=crop&w=800&q=80',
        variantId: 'var-bath-1',
        variantName: 'Single Bathroom Complete Scrub',
        professionalId: 'pro-amit',
        professionalName: 'Amit Rawat',
        professionalPhone: '+91 97123 99887',
        professionalAvatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80',
        professionalRating: 4.88,
        professionalProfession: 'Deep Cleaning Master Crew Lead',
        date: '2026-09-10',
        timeSlot: '02:00 PM - 03:00 PM',
        address: defaultAddr,
        status: 'COMPLETED',
        statusHistory: [
          { status: 'CONFIRMED', timestamp: '2026-09-10 11:00 AM', note: 'Booking confirmed' },
          { status: 'ASSIGNED', timestamp: '2026-09-10 11:20 AM', note: 'Amit Rawat assigned' },
          { status: 'ON_THE_WAY', timestamp: '2026-09-10 01:30 PM', note: 'Partner en route' },
          { status: 'ARRIVED', timestamp: '2026-09-10 01:55 PM', note: 'Partner arrived at premises' },
          { status: 'IN_PROGRESS', timestamp: '2026-09-10 02:05 PM', note: 'Cleaning in progress with rotary scrub' },
          { status: 'COMPLETED', timestamp: '2026-09-10 03:15 PM', note: 'Service completed to 100% satisfaction' },
        ],
        subtotal: 449,
        discount: 0,
        taxes: 22,
        total: 471,
        paymentMethod: 'CARD',
        paymentStatus: 'PAID',
        createdAt: '2026-09-10 11:00 AM',
      },
    ];
  }

  private seedInitialNotifications() {
    this.notifications = [
      {
        id: 'notif-1',
        title: 'Partner On The Way! 🛵',
        message: 'Rahul Sharma is on the way for Foam Jet Power AC Servicing. ETA: 15 mins.',
        type: 'PRO',
        read: false,
        timestamp: '10 mins ago',
        link: '/track/bk-1001',
      },
      {
        id: 'notif-2',
        title: 'Booking Confirmed ✨',
        message: 'Your booking #SRV-2026-8941 has been confirmed for Sep 17, 11:00 AM.',
        type: 'BOOKING',
        read: true,
        timestamp: '2 hours ago',
        link: '/booking/bk-1001',
      },
      {
        id: 'notif-3',
        title: 'Special Offer: ₹150 OFF 🎉',
        message: 'Use coupon WELCOME150 on your first booking to save ₹150.',
        type: 'OFFER',
        read: true,
        timestamp: 'Yesterday',
        link: '/offers',
      },
    ];
  }

  private seedInitialTickets() {
    this.tickets = [
      {
        id: 'tkt-1',
        ticketNumber: 'TKT-99201',
        userId: 'usr-customer-1',
        userName: 'Priya Sharma',
        userEmail: 'priya.sharma@example.com',
        subject: 'Can I reschedule my appointment slot?',
        category: 'Booking',
        message: 'Hi, I might need to shift my afternoon slot by 1 hour. How do I do that?',
        status: 'RESOLVED',
        createdAt: '2026-09-12 10:00 AM',
        replies: [
          {
            id: 'rep-1',
            sender: 'Customer Support Lead',
            role: 'SUPPORT',
            message: 'Hello Priya! You can reschedule free of charge up to 2 hours before the scheduled time directly from your Bookings tab.',
            timestamp: '2026-09-12 10:15 AM',
          },
        ],
      },
    ];
  }

  // --- Users ---
  getUserById(id: string) {
    return this.users.find((u) => u.id === id);
  }

  getUserByEmail(email: string) {
    return this.users.find((u) => u.email.toLowerCase() === email.toLowerCase());
  }

  createUser(userData: Partial<User>) {
    const newUser: User = {
      id: `usr-${Date.now()}`,
      name: userData.name || 'New User',
      email: userData.email || '',
      phone: userData.phone || '+91 98765 00000',
      role: userData.role || 'CUSTOMER',
      avatar: userData.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(userData.name || 'User')}`,
      addresses: userData.addresses || [],
      createdAt: new Date().toISOString().split('T')[0],
    };
    this.users.push(newUser);
    return newUser;
  }

  addAddress(userId: string, address: Omit<Address, 'id'>) {
    const user = this.getUserById(userId);
    if (!user) return null;
    const newAddress: Address = {
      ...address,
      id: `addr-${Date.now()}`,
    };
    if (newAddress.isDefault) {
      user.addresses.forEach((a) => (a.isDefault = false));
    }
    user.addresses.push(newAddress);
    return newAddress;
  }

  // --- Categories & Services ---
  getCategories() {
    return this.categories;
  }

  getServices(categoryId?: string, search?: string) {
    let list = [...this.services];
    if (categoryId) {
      list = list.filter((s) => s.categoryId === categoryId);
    }
    if (search) {
      const q = search.toLowerCase();
      list = list.filter(
        (s) =>
          s.name.toLowerCase().includes(q) ||
          s.shortDesc.toLowerCase().includes(q) ||
          s.categoryName.toLowerCase().includes(q)
      );
    }
    return list;
  }

  getServiceBySlug(slug: string) {
    return this.services.find((s) => s.slug === slug);
  }

  getServiceById(id: string) {
    return this.services.find((s) => s.id === id);
  }

  // --- Professionals ---
  getProfessionals(categoryId?: string) {
    if (categoryId) {
      return this.professionals.filter((p) => p.categoryId === categoryId);
    }
    return this.professionals;
  }

  getProfessionalById(id: string) {
    return this.professionals.find((p) => p.id === id);
  }

  updateProfessionalAvailability(id: string, isAvailableToday: boolean) {
    const pro = this.getProfessionalById(id);
    if (pro) {
      pro.isAvailableToday = isAvailableToday;
    }
    return pro;
  }

  // --- Bookings ---
  createBooking(bookingData: Partial<Booking>) {
    const id = `bk-${Date.now()}`;
    const bookingNumber = `SRV-2026-${Math.floor(1000 + Math.random() * 9000)}`;
    
    // Assign a matching professional if available
    const matchingPro = this.professionals.find((p) => p.isAvailableToday) || this.professionals[0];

    const newBooking: Booking = {
      id,
      bookingNumber,
      customerId: bookingData.customerId || 'usr-customer-1',
      customerName: bookingData.customerName || 'Customer',
      customerPhone: bookingData.customerPhone || '+91 98765 12345',
      serviceId: bookingData.serviceId || '',
      serviceName: bookingData.serviceName || '',
      serviceSlug: bookingData.serviceSlug || '',
      serviceImage: bookingData.serviceImage || '',
      variantId: bookingData.variantId || '',
      variantName: bookingData.variantName || '',
      professionalId: matchingPro.id,
      professionalName: matchingPro.name,
      professionalPhone: matchingPro.phone,
      professionalAvatar: matchingPro.avatar,
      professionalRating: matchingPro.rating,
      professionalProfession: matchingPro.profession,
      date: bookingData.date || new Date().toISOString().split('T')[0],
      timeSlot: bookingData.timeSlot || '10:00 AM - 11:00 AM',
      address: bookingData.address || DEMO_USERS[0].addresses[0],
      instructions: bookingData.instructions || '',
      status: 'CONFIRMED',
      statusHistory: [
        { status: 'CONFIRMED', timestamp: new Date().toLocaleString(), note: 'Booking confirmed & slot reserved' },
        { status: 'ASSIGNED', timestamp: new Date().toLocaleString(), note: `${matchingPro.name} assigned to your service` },
      ],
      subtotal: bookingData.subtotal || 0,
      discount: bookingData.discount || 0,
      couponApplied: bookingData.couponApplied,
      taxes: bookingData.taxes || 0,
      total: bookingData.total || 0,
      paymentMethod: bookingData.paymentMethod || 'UPI',
      paymentStatus: bookingData.paymentMethod === 'CASH' ? 'PENDING' : 'PAID',
      createdAt: new Date().toLocaleString(),
      estimatedArrival: '30 mins',
    };

    this.bookings.unshift(newBooking);

    // Also push a notification
    this.notifications.unshift({
      id: `notif-${Date.now()}`,
      title: 'Booking Confirmed 🎉',
      message: `Your appointment for ${newBooking.serviceName} is confirmed for ${newBooking.date} at ${newBooking.timeSlot}.`,
      type: 'BOOKING',
      read: false,
      timestamp: 'Just now',
      link: `/track/${newBooking.id}`,
    });

    return newBooking;
  }

  getBookingById(id: string) {
    return this.bookings.find((b) => b.id === id);
  }

  getBookingsByCustomerId(customerId: string) {
    return this.bookings.filter((b) => b.customerId === customerId);
  }

  getBookingsByProfessionalId(professionalId: string) {
    return this.bookings.filter((b) => b.professionalId === professionalId);
  }

  getAllBookings() {
    return this.bookings;
  }

  updateBookingStatus(id: string, status: BookingStatus, note?: string) {
    const booking = this.getBookingById(id);
    if (!booking) return null;
    booking.status = status;
    booking.statusHistory.push({
      status,
      timestamp: new Date().toLocaleString(),
      note: note || `Booking marked as ${status.replace('_', ' ')}`,
    });

    if (status === 'COMPLETED') {
      booking.paymentStatus = 'PAID';
    }

    // Trigger notification
    this.notifications.unshift({
      id: `notif-${Date.now()}`,
      title: `Status: ${status.replace(/_/g, ' ')}`,
      message: `Booking #${booking.bookingNumber} updated to ${status.replace(/_/g, ' ')}.`,
      type: 'BOOKING',
      read: false,
      timestamp: 'Just now',
      link: `/track/${booking.id}`,
    });

    return booking;
  }

  // --- Reviews ---
  addReview(reviewData: Partial<Review>) {
    const newReview: Review = {
      id: `rev-${Date.now()}`,
      bookingId: reviewData.bookingId || '',
      serviceId: reviewData.serviceId || '',
      serviceName: reviewData.serviceName || '',
      professionalId: reviewData.professionalId,
      professionalName: reviewData.professionalName,
      customerName: reviewData.customerName || 'Customer',
      customerAvatar: reviewData.customerAvatar,
      customerCity: reviewData.customerCity || 'Agra',
      rating: reviewData.rating || 5,
      comment: reviewData.comment || '',
      tags: reviewData.tags || ['Quality', 'Professional'],
      date: new Date().toISOString().split('T')[0],
      verifiedBooking: true,
    };
    this.reviews.unshift(newReview);

    // Update booking with review
    if (reviewData.bookingId) {
      const b = this.getBookingById(reviewData.bookingId);
      if (b) {
        b.review = newReview;
      }
    }

    return newReview;
  }

  getReviews(serviceId?: string, professionalId?: string) {
    let list = [...this.reviews];
    if (serviceId) {
      list = list.filter((r) => r.serviceId === serviceId);
    }
    if (professionalId) {
      list = list.filter((r) => r.professionalId === professionalId);
    }
    return list;
  }

  // --- Coupons ---
  getCoupons() {
    return this.coupons;
  }

  validateCoupon(code: string, amount: number, categoryId?: string) {
    const coupon = this.coupons.find((c) => c.code.toUpperCase() === code.toUpperCase().trim());
    if (!coupon) {
      return { valid: false, message: 'Invalid coupon code.' };
    }
    if (amount < coupon.minBookingAmount) {
      return { valid: false, message: `Minimum booking value of ₹${coupon.minBookingAmount} required for this code.` };
    }
    if (coupon.categoryLimit && categoryId && coupon.categoryLimit !== categoryId) {
      return { valid: false, message: 'This coupon is not valid for the selected category.' };
    }

    let discountAmount = 0;
    if (coupon.discountType === 'FLAT') {
      discountAmount = coupon.value;
    } else {
      discountAmount = Math.round((amount * coupon.value) / 100);
      if (coupon.maxDiscount && discountAmount > coupon.maxDiscount) {
        discountAmount = coupon.maxDiscount;
      }
    }

    return {
      valid: true,
      coupon,
      discountAmount,
      message: `Success! You saved ₹${discountAmount} with code ${coupon.code}`,
    };
  }

  // --- Notifications ---
  getNotifications() {
    return this.notifications;
  }

  markNotificationRead(id: string) {
    const notif = this.notifications.find((n) => n.id === id);
    if (notif) notif.read = true;
    return notif;
  }

  // --- Support ---
  createSupportTicket(ticketData: Partial<SupportTicket>) {
    const newTicket: SupportTicket = {
      id: `tkt-${Date.now()}`,
      ticketNumber: `TKT-${Math.floor(10000 + Math.random() * 90000)}`,
      userId: ticketData.userId || 'usr-customer-1',
      userName: ticketData.userName || 'Customer',
      userEmail: ticketData.userEmail || 'customer@serviceassist.in',
      subject: ticketData.subject || 'General Inquiry',
      category: ticketData.category || 'General',
      message: ticketData.message || '',
      status: 'OPEN',
      createdAt: new Date().toLocaleString(),
      replies: [
        {
          id: `rep-${Date.now()}`,
          sender: 'Service Assist Support Bot',
          role: 'SUPPORT',
          message: 'Thank you for contacting Service Assist support. Our team has received your ticket and an agent will respond within 30 minutes.',
          timestamp: new Date().toLocaleString(),
        },
      ],
    };
    this.tickets.unshift(newTicket);
    return newTicket;
  }

  getTickets(userId?: string) {
    if (userId) {
      return this.tickets.filter((t) => t.userId === userId);
    }
    return this.tickets;
  }

  // --- Admin Analytics ---
  getAdminMetrics() {
    const totalBookings = this.bookings.length;
    const completedBookings = this.bookings.filter((b) => b.status === 'COMPLETED').length;
    const totalRevenue = this.bookings
      .filter((b) => b.paymentStatus === 'PAID')
      .reduce((sum, b) => sum + b.total, 0);
    const activeProfessionals = this.professionals.filter((p) => p.isAvailableToday).length;
    const totalUsers = this.users.length;

    // Monthly revenue simulation data for Recharts
    const revenueTrend = [
      { month: 'Apr', revenue: 42000, bookings: 78 },
      { month: 'May', revenue: 58000, bookings: 104 },
      { month: 'Jun', revenue: 84000, bookings: 156 },
      { month: 'Jul', revenue: 99000, bookings: 182 },
      { month: 'Aug', revenue: 115000, bookings: 210 },
      { month: 'Sep', revenue: 148000, bookings: 275 },
    ];

    const categoryBreakdown = [
      { name: 'AC & Appliances', value: 42, color: '#0EA5E9' },
      { name: 'Cleaning & Pest', value: 28, color: '#10B8A8' },
      { name: 'Beauty & Salon', value: 18, color: '#EC4899' },
      { name: 'Electrician & Repairs', value: 12, color: '#F59E0B' },
    ];

    return {
      totalBookings,
      completedBookings,
      totalRevenue,
      activeProfessionals,
      totalUsers,
      averageRating: 4.88,
      cancellationRate: '2.4%',
      revenueTrend,
      categoryBreakdown,
      cities: CITIES,
    };
  }
}

export const db = new DatabaseStore();
