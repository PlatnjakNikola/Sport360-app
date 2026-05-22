package com.sport360.moduleservice.clients.service

import com.sport360.moduleservice.clients.domain.Client
import com.sport360.moduleservice.clients.repository.ClientRepository
import com.sport360.moduleservice.clients.web.ClientResponse
import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.common.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminClientService(private val clientRepository: ClientRepository) {

    @Transactional(readOnly = true)
    fun list(pageable: Pageable): PageResponse<ClientResponse> =
        PageResponse.from(clientRepository.findAllByOrderByUserIdAsc(pageable)) { it.toResponse() }

    @Transactional(readOnly = true)
    fun get(id: Long): ClientResponse =
        clientRepository.findById(id).orElseThrow { NotFoundException("Client not found") }.toResponse()

    private fun Client.toResponse() = ClientResponse(
        userId = userId,
        companyName = companyName,
        contactName = user.name,
        email = user.email,
        contactPhone = contactPhone,
        address = address,
        active = user.isActive,
        createdAt = user.createdAt,
    )
}
