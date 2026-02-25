package lobby.minecraft_minigames.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Party {

    private UUID leader;
    private final Set<UUID> members;
    private final Set<UUID> pendingInvites;
    private static final int MAX_SIZE = 8;

    public Party(UUID leader) {
        this.leader = leader;
        this.members = new HashSet<>();
        this.members.add(leader);
        this.pendingInvites = new HashSet<>();
    }

    public UUID getLeader() { return leader; }

    public void setLeader(UUID leader) { this.leader = leader; }

    public Set<UUID> getMembers() { return members; }

    public boolean isMember(UUID uuid) { return members.contains(uuid); }

    public boolean isLeader(UUID uuid) { return leader.equals(uuid); }

    public boolean isFull() { return members.size() >= MAX_SIZE; }

    public int size() { return members.size(); }

    public boolean addMember(UUID uuid) {
        if (isFull()) return false;
        pendingInvites.remove(uuid);
        return members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        if (leader.equals(uuid) && !members.isEmpty()) {
            leader = members.iterator().next();
        }
    }

    public void invite(UUID uuid) {
        pendingInvites.add(uuid);
    }

    public boolean hasInvite(UUID uuid) {
        return pendingInvites.contains(uuid);
    }

    public void revokeInvite(UUID uuid) {
        pendingInvites.remove(uuid);
    }

    public Set<UUID> getPendingInvites() { return pendingInvites; }
}
