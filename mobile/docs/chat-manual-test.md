# Servora / ServiceAssist — Chat Manual Verification Protocol

## 1. Test Setup
- **Device 1 / Session 1**: Priya Sharma (Customer, `user_priya_1`)
- **Device 2 / Session 2**: Rajesh Sharma (Partner / Professional, `pro_rajesh_1`)
- **Admin Session**: Sneha Patel (Admin, `user_admin_sneha`)

---

## 2. Step-by-Step Test Procedure

| Step | Action | Expected Result | Verification Check |
|---|---|---|---|
| **1** | Priya sends message: `"Gate is open, please ring bell"` | Outgoing bubble displays text, local timestamp (e.g. `4:15 PM`), and single grey clock (SENDING) transitioning to single grey tick (SENT). Optimistic message smoothly reconciles without duplicating. | ✅ Single grey tick on Priya's message |
| **2** | Rajesh's app is open in foreground (on Jobs tab or Home) | Within ~4 seconds (`chat-sync` heartbeat), Rajesh's Chats tab badge increments `0 -> 1`. A heads-up banner `"Priya · AC Service: Gate is open..."` appears. The conversation moves to the top of Rajesh's Chats list. | ✅ Badge 1, Banner shown, top of list |
| **3** | Check Priya's screen while Rajesh is on Jobs tab | Priya's message tick transitions from single grey tick to two grey ticks (DELIVERED) because Rajesh's app synced the message via `chat-sync`. | ✅ Two grey ticks (Delivered) |
| **4** | Rajesh opens the chat thread | Thread opens, messages display oldest to newest with day dividers (`Today`). Within 3s, `chat-ack` updates `last_read_seq`. | ✅ Thread loaded cleanly |
| **5** | Check Priya's screen after Rajesh views thread | Priya's outgoing message ticks transition from two grey ticks to two blue ticks (READ, `#34B7F1`). | ✅ Two blue ticks (Read) |
| **6** | Rajesh replies: `"On my way, arriving in 5 minutes"` | Reply appears instantly in Rajesh's thread with single grey tick. Priya receives the message within 4s, chat list bumps to top with `"Rajesh: On my way..."`, unread badge increments. | ✅ Bi-directional delivery confirmed |
| **7** | Two conversations test | Open another booking conversation and send a message. That conversation immediately moves above older conversations in the Chats list. | ✅ Newest-first sorting verified |
| **8** | Timezone test | Change device time zone. Timestamps update to reflect local time accurately without "Now" or "Just now" placeholders. | ✅ 12-hour local time format verified |
| **9** | Admin audit view | Admin Sneha logs in and views the conversation in Admin Console. Both parties' timestamps and status ticks are visible. Admin viewing does NOT trigger receipt writes (never turns unread to read). | ✅ Read-only audit compliance verified |
| **10** | Logout and switch user | Log out of Priya's account and log in as Rajesh on the same device. Priya's thread, messages, and state are completely cleared; Rajesh's own assigned chats load fresh. | ✅ Zero state leakage across sessions |
