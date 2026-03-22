package com.example.querysence.service;

import com.example.querysence.model.*;
import com.example.querysence.model.dto.*;
import com.example.querysence.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectCollaborationService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectInviteRepository projectInviteRepository;

    @Autowired
    private UserRepository userRepository;

    // Get list of project members
    public List<ProjectMemberDto> getProjectMembers(Long projectId) {
        List<ProjectMember> members = projectMemberRepository.findByProjectId(projectId);
        return members.stream()
            .map(member -> ProjectMemberDto.builder()
                .id(member.getId())
                .userId(member.getUser().getId())
                .email(member.getUser().getEmail())
                .fullName(member.getUser().getFullName())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build())
            .collect(Collectors.toList());
    }

    // Create an invite link
    public ProjectInviteDto createInvite(Long projectId, CreateInviteRequest request, Authentication auth) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        User currentUser = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Check permission: only owner/editor can invite
        ProjectMember currentMember = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this project"));

        if (currentMember.getRole() == ProjectRole.VIEWER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner/editor can invite members");
        }

        // Check if invite already exists for this email
        List<ProjectInvite> existingInvites = projectInviteRepository.findByEmailAndIsUsedFalse(request.getEmail());
        for (ProjectInvite invite : existingInvites) {
            if (invite.getProject().getId().equals(projectId) && !invite.getIsUsed()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite already exists for this email");
            }
        }

        // Check if user is already a member
        User invitedUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (invitedUser != null) {
            projectMemberRepository.findByProjectIdAndUserId(projectId, invitedUser.getId())
                .ifPresent(m -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is already a member");
                });
        }

        // Create invite
        ProjectInvite invite = ProjectInvite.builder()
            .project(project)
            .email(request.getEmail())
            .inviteCode(UUID.randomUUID().toString())
            .role(request.getRole() != null ? request.getRole() : ProjectRole.VIEWER)
            .createdBy(currentUser)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .isUsed(false)
            .build();

        projectInviteRepository.save(invite);

        return ProjectInviteDto.builder()
            .id(invite.getId())
            .inviteCode(invite.getInviteCode())
            .email(invite.getEmail())
            .role(invite.getRole())
            .createdByEmail(invite.getCreatedBy().getEmail())
            .expiresAt(invite.getExpiresAt())
            .createdAt(invite.getCreatedAt())
            .isUsed(invite.getIsUsed())
            .build();
    }

    // Accept an invite
    public ProjectMemberDto acceptInvite(String inviteCode, Authentication auth) {
        ProjectInvite invite = projectInviteRepository.findByInviteCode(inviteCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found"));

        if (invite.getIsUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite has already been used");
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite has expired");
        }

        User currentUser = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!currentUser.getEmail().equals(invite.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This invite is not for your email");
        }

        // Check if user is already a member
        projectMemberRepository.findByProjectIdAndUserId(invite.getProject().getId(), currentUser.getId())
            .ifPresent(m -> {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are already a member of this project");
            });

        // Add user to project
        ProjectMember member = ProjectMember.builder()
            .project(invite.getProject())
            .user(currentUser)
            .role(invite.getRole())
            .build();

        projectMemberRepository.save(member);

        // Mark invite as used
        invite.setIsUsed(true);
        invite.setAcceptedBy(currentUser);
        invite.setAcceptedAt(LocalDateTime.now());
        projectInviteRepository.save(invite);

        return ProjectMemberDto.builder()
            .id(member.getId())
            .userId(member.getUser().getId())
            .email(member.getUser().getEmail())
            .fullName(member.getUser().getFullName())
            .role(member.getRole())
            .joinedAt(member.getJoinedAt())
            .build();
    }

    // Update member role
    public ProjectMemberDto updateMemberRole(Long projectId, Long memberId, ProjectRole newRole, Authentication auth) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        User currentUser = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Check permission
        ProjectMember currentMember = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this project"));

        if (currentMember.getRole() == ProjectRole.VIEWER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner/editor can manage members");
        }

        ProjectMember member = projectMemberRepository.findById(memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (!member.getProject().getId().equals(projectId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Member does not belong to this project");
        }

        member.setRole(newRole);
        projectMemberRepository.save(member);

        return ProjectMemberDto.builder()
            .id(member.getId())
            .userId(member.getUser().getId())
            .email(member.getUser().getEmail())
            .fullName(member.getUser().getFullName())
            .role(member.getRole())
            .joinedAt(member.getJoinedAt())
            .build();
    }

    // Remove member from project
    public void removeMember(Long projectId, Long memberId, Authentication auth) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        User currentUser = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Check permission
        ProjectMember currentMember = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this project"));

        if (currentMember.getRole() == ProjectRole.VIEWER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner/editor can remove members");
        }

        ProjectMember member = projectMemberRepository.findById(memberId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));

        if (!member.getProject().getId().equals(projectId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Member does not belong to this project");
        }

        // Cannot remove owner
        if (member.getRole() == ProjectRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove project owner");
        }

        projectMemberRepository.deleteById(memberId);
    }

    // Get project invites
    public List<ProjectInviteDto> getProjectInvites(Long projectId, Authentication auth) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        User currentUser = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Check permission
        ProjectMember currentMember = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUser.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this project"));

        if (currentMember.getRole() == ProjectRole.VIEWER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner/editor can view invites");
        }

        List<ProjectInvite> invites = projectInviteRepository.findByProjectId(projectId);
        return invites.stream()
            .map(invite -> ProjectInviteDto.builder()
                .id(invite.getId())
                .inviteCode(invite.getInviteCode())
                .email(invite.getEmail())
                .role(invite.getRole())
                .createdByEmail(invite.getCreatedBy().getEmail())
                .expiresAt(invite.getExpiresAt())
                .createdAt(invite.getCreatedAt())
                .isUsed(invite.getIsUsed())
                .build())
            .collect(Collectors.toList());
    }

    // Get pending invites for a user by email
    public List<ProjectInviteDto> getPendingInvitesByEmail(String email) {
        List<ProjectInvite> invites = projectInviteRepository.findByEmailAndIsUsedFalse(email);
        return invites.stream()
            .map(invite -> ProjectInviteDto.builder()
                .id(invite.getId())
                .inviteCode(invite.getInviteCode())
                .email(invite.getEmail())
                .role(invite.getRole())
                .projectId(invite.getProject().getId())
                .createdByEmail(invite.getCreatedBy().getEmail())
                .expiresAt(invite.getExpiresAt())
                .createdAt(invite.getCreatedAt())
                .isUsed(invite.getIsUsed())
                .build())
            .collect(Collectors.toList());
    }
}
