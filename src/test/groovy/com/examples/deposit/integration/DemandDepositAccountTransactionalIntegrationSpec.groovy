package com.examples.deposit.integration

import com.examples.deposit.domain.DemandDepositAccountLifecycleEvent
import com.examples.deposit.repository.DemandDepositAccountLifecycleEventRepository
import com.examples.deposit.repository.DemandDepositAccountRepository
import com.examples.deposit.service.DemandDepositAccountService
import com.examples.deposit.service.dto.CreateDemandDepositAccountCommand
import com.github.f4b6a3.uuid.alt.GUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.spockframework.spring.SpringBean
import spock.lang.Specification

@SpringBootTest
class DemandDepositAccountTransactionalIntegrationSpec extends Specification {

    @Autowired
    private DemandDepositAccountService service

    @Autowired
    private DemandDepositAccountRepository accountRepository

    @SpringBean
    private DemandDepositAccountLifecycleEventRepository lifecycleEventRepository = Mock()

    def "createMainAccount rolls back account when lifecycle event persistence fails"() {
        given:
        def customerId = GUID.v7().toUUID()
        def idempotencyKey = "idem-integration-atomicity-001"
        def command = new CreateDemandDepositAccountCommand(customerId, idempotencyKey)

        when:
        service.createMainAccount(command)

        then:
        1 * lifecycleEventRepository.save(_ as DemandDepositAccountLifecycleEvent) >> {
            throw new DataIntegrityViolationException("simulated lifecycle event persistence failure")
        }
        thrown(DataIntegrityViolationException)
        accountRepository.findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey).empty
    }
}
