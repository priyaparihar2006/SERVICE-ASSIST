// Thin registry around the Socket.IO server so services can push events without importing it.
// Every method is a no-op until attachRealtime() registers a server (e.g. in REST-only tests).
// Presence is derived from this process's rooms; running several API instances needs a shared
// adapter (e.g. Redis) and presence store.
let io = null;
const userRoom = (userId) => `user:${userId}`;
export const sessionRoom = (sessionId) => `session:${sessionId}`;

export const hub = {
  attach(server) {
    io = server;
  },
  detach() {
    io = null;
  },
  userRoom,
  emitToUser(userId, event, payload) {
    io?.to(userRoom(userId)).emit(event, payload);
  },
  isOnline(userId) {
    return (io?.sockets.adapter.rooms.get(userRoom(userId))?.size || 0) > 0;
  },
  connectionCount(userId) {
    return io?.sockets.adapter.rooms.get(userRoom(userId))?.size || 0;
  },
  disconnectSession(sessionId) {
    io?.in(sessionRoom(sessionId)).disconnectSockets(true);
  },
  disconnectUser(userId) {
    io?.in(userRoom(userId)).disconnectSockets(true);
  },
};
