package com.pinterest.teletraan.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.stream.Stream;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.pinterest.deployservice.bean.PromoteBean;
import com.pinterest.deployservice.bean.PromoteDisablePolicy;
import com.pinterest.deployservice.bean.PromoteType;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.teletraan.TeletraanServiceContext;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;

@ExtendWith(DropwizardExtensionsSupport.class)
public class EnvPromotesTest {

    private static final ResourceExtension EXT;

    static {
        TeletraanServiceContext context = new TeletraanServiceContext();

        EXT = ResourceExtension.builder()
                .addResource(new EnvPromotes(context))
                .build();
    }

    @ParameterizedTest
    @MethodSource("invalidPromoteBeansSource")
    public void invalidPromoteBeans_422(PromoteBean promoteBean) {
        final Response put = EXT.target(Target.V1_ENV_PROMOTES).request().put(Entity.json(promoteBean));

        assertEquals(422, put.getStatus());
    }

    @ParameterizedTest
    @MethodSource("validPromoteBeansSource")
    public void invalidPromoteBeans_not422(PromoteBean promoteBean) {
        final Response put = EXT.target(Target.V1_ENV_PROMOTES).request().put(Entity.json(promoteBean));

        assertNotEquals(422, put.getStatus());
    }

    static Stream<Arguments> invalidPromoteBeansSource() {
        PromoteBean negativeQueueSize = validPromoteBean();
        negativeQueueSize.setQueue_size(-1);
        PromoteBean largeQueueSize = validPromoteBean();
        largeQueueSize.setQueue_size(Constants.DEFAULT_MAX_PROMOTE_QUEUE_SIZE + 1);
        PromoteBean negativeDelay = validPromoteBean();
        negativeDelay.setDelay(-1);
        PromoteBean invalidCron = validPromoteBean();
        invalidCron.setSchedule("invalidCron");
        PromoteBean invalidCron2 = validPromoteBean();
        invalidCron2.setSchedule("0 0 0 * * *");

        return Stream.of(
                Arguments.of(new PromoteBean()),
                Arguments.of(negativeQueueSize),
                Arguments.of(largeQueueSize),
                Arguments.of(negativeDelay),
                Arguments.of(invalidCron),
                Arguments.of(invalidCron2)
        );
    }

    static Stream<Arguments> validPromoteBeansSource() {
        PromoteBean manual = validPromoteBean();
        manual.setType(PromoteType.MANUAL);
        manual.setSchedule("invalidCron");
        PromoteBean emptySchedule = validPromoteBean();
        emptySchedule.setSchedule(" ");
        PromoteBean validSchedule = validPromoteBean();
        validSchedule.setSchedule("0 0 0 * * ?");

        return Stream.of(
                Arguments.of(validPromoteBean()),
                Arguments.of(manual),
                Arguments.of(validSchedule),
                Arguments.of(emptySchedule)
        );
    }

    static PromoteBean validPromoteBean() {
        PromoteBean promoteBean = new PromoteBean();
        promoteBean.setQueue_size(1);
        promoteBean.setDelay(0);
        promoteBean.setType(PromoteType.AUTO);
        promoteBean.setDisable_policy(PromoteDisablePolicy.AUTO);
        return promoteBean;
    }

    private static class Target {
        private static final String V1_ENV_PROMOTES = "v1/envs/testEnv/testStage/promotes";
    }
}
