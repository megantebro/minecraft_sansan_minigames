package lobby.minecraft_minigames.manager;

import lobby.minecraft_minigames.model.Party;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyManager {

    private final Map<UUID, Party> parties;        // leaderId -> Party
    private final Map<UUID, UUID> playerPartyMap;  // playerId -> leaderId

    public PartyManager() {
        this.parties = new HashMap<>();
        this.playerPartyMap = new HashMap<>();
    }

    public Party createParty(UUID leader) {
        if (playerPartyMap.containsKey(leader)) return null;
        Party party = new Party(leader);
        parties.put(leader, party);
        playerPartyMap.put(leader, leader);
        return party;
    }

    public void disbandParty(UUID leaderId) {
        Party party = parties.remove(leaderId);
        if (party == null) return;
        for (UUID member : party.getMembers()) {
            playerPartyMap.remove(member);
        }
    }

    public boolean invitePlayer(UUID leaderId, UUID target) {
        Party party = parties.get(leaderId);
        if (party == null || !party.isLeader(leaderId)) return false;
        if (party.isFull()) return false;
        if (playerPartyMap.containsKey(target)) return false;
        party.invite(target);
        return true;
    }

    public boolean acceptInvite(UUID playerId, UUID partyLeaderId) {
        if (playerPartyMap.containsKey(playerId)) return false;
        Party party = parties.get(partyLeaderId);
        if (party == null || !party.hasInvite(playerId)) return false;
        if (party.isFull()) return false;
        party.addMember(playerId);
        playerPartyMap.put(playerId, partyLeaderId);
        return true;
    }

    public void denyInvite(UUID playerId, UUID partyLeaderId) {
        Party party = parties.get(partyLeaderId);
        if (party != null) {
            party.revokeInvite(playerId);
        }
    }

    public void leaveParty(UUID playerId) {
        UUID leaderId = playerPartyMap.remove(playerId);
        if (leaderId == null) return;
        Party party = parties.get(leaderId);
        if (party == null) return;
        party.removeMember(playerId);

        if (party.size() == 0) {
            parties.remove(leaderId);
        } else if (playerId.equals(leaderId)) {
            // Leadership transferred in Party.removeMember
            parties.remove(leaderId);
            parties.put(party.getLeader(), party);
            // Update all members' party map
            for (UUID member : party.getMembers()) {
                playerPartyMap.put(member, party.getLeader());
            }
        }
    }

    public void kickPlayer(UUID leaderId, UUID targetId) {
        Party party = parties.get(leaderId);
        if (party == null || !party.isLeader(leaderId)) return;
        party.removeMember(targetId);
        playerPartyMap.remove(targetId);
    }

    public Party getParty(UUID playerId) {
        UUID leaderId = playerPartyMap.get(playerId);
        if (leaderId == null) return null;
        return parties.get(leaderId);
    }

    public boolean isInParty(UUID playerId) {
        return playerPartyMap.containsKey(playerId);
    }

    public boolean isLeader(UUID playerId) {
        Party party = getParty(playerId);
        return party != null && party.isLeader(playerId);
    }

    public UUID findPartyWithInvite(UUID playerId) {
        for (Map.Entry<UUID, Party> entry : parties.entrySet()) {
            if (entry.getValue().hasInvite(playerId)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
