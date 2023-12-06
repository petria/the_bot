package org.freakz.engine.commands.handlers.topcount;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.engine.EngineRequest;
import org.freakz.common.util.Uptime;
import org.freakz.data.service.DataValuesService;
import org.freakz.dto.GetDataValuesServiceResponse;
import org.freakz.engine.commands.annotations.HokanCommandHandler;
import org.freakz.engine.commands.api.AbstractCmd;
import org.freakz.services.api.ServiceRequestType;


@HokanCommandHandler
@Slf4j
public class JanoCmd extends AbstractCmd {

    @Override
    public void initCommandOptions(JSAP jsap) throws JSAPException {
        jsap.setHelp("How thirsty you are!? When did you last *glugga* !??!");
    }

    @Override
    public String executeCommand(EngineRequest request, JSAPResult results) {
        String nick = request.getFromSender();
        String channel = request.getReplyTo();
        String network = request.getNetwork();
        String key = String.format("%s_LAST_GLUGGA", nick.toUpperCase());

        GetDataValuesServiceResponse serviceResponse = doServiceRequest(request, results, ServiceRequestType.GetDataValuesService);
        DataValuesService dataValuesService = serviceResponse.getDataValuesService();

        String value = dataValuesService.getValue(nick, channel, network, key);
        if (value != null) {
            Uptime uptime = new Uptime(Long.parseLong(value));
            long future = System.currentTimeMillis();
            Integer[] td = uptime.getTimeDiff(future);
            String days = getValue(td[3], 0, "day");
            String hours = getValue(td[2], 0, "hour");
            String minutes = getValue(td[1], 0, "minute");
            String second = getValue(td[0], -1, "second");

            String noGlugga = String.format("%s%s%s%s", days, hours, minutes, second);
            return String.format("Your last *glugga*: %sago!!", noGlugga);

        } else {
            return String.format("No *glugga* no fun!");
        }

    }

    private String getValue(Integer value, int compare, String str) {
        String ret = "";
        if (value > compare) {
            String many = value > 1 ? "s" : "";
            ret = String.format("%d %s%s ", value, str, many);
        }
        return ret;
    }
}
