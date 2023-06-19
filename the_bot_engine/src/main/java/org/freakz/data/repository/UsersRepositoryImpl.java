package org.freakz.data.repository;

import lombok.extern.slf4j.Slf4j;
import org.freakz.common.model.dto.DataNodeBase;
import org.freakz.common.model.dto.DataValuesJsonContainer;
import org.freakz.common.model.users.User;
import org.freakz.config.ConfigService;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class UsersRepositoryImpl extends RepositoryBaseImpl implements UsersRepository, DataSavingService {


    private static final String USERS_FILE_NAME = "users.json";

    public UsersRepositoryImpl(ConfigService configService) {
        super(configService);
        createUsers();
    }

    private void createUsers() {

        this.dataValues.add(getJohnDoeUser());

        User user
                = User.builder()
                .isAdmin(true)
                .name("Petri Airio")
                .email("petri.j.airio@gmail.com")
                .ircNick("_Pete_")
                .discordId("265828694445129728")
                .telegramId("138695441")
                .build();
        user.setId(++highestId);
        this.dataValues.add(user);

        isDirty = true;

    }

    public void saveDataValues() throws IOException {
        synchronized (this.dataValues) {
            String dataFileName = configService.getRuntimeDataFileName(USERS_FILE_NAME);
            log.debug("synchronized start writing values: {}", dataFileName);

            DataValuesJsonContainer container
                    = DataValuesJsonContainer.builder()
                    .data_values(this.dataValues)
                    .build();
            container.setSaveTimes(container.getSaveTimes() + 1);

            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(container);
            Files.writeString(Path.of(dataFileName), json, Charset.defaultCharset());

            log.debug("synchronized block write done: {}", this.dataValues.size());
        }
    }

    @Override
    public void checkIsSavingNeeded() {
        if (this.saveTrigger >= 0) {
            this.saveTrigger = this.saveTrigger - 100;
        }
        if (isDirty) {
            if (saveTrigger <= 0) {
                try {
                    saveDataValues();
                    isDirty = false;
                } catch (IOException e) {
                    log.error("Saving data values failed", e);
                }
            }
        }
    }

    @Override
    public DataSaverInfo getDataSaverInfo() {
        DataSaverInfo info
                = DataSaverInfo.builder()
                .name("UsersData")
                .build();
        return info;
    }

    private User getJohnDoeUser() {
        User user = User.builder()
                .isAdmin(false)
                .name("John Doe")
                .email("none@invalid")
                .ircNick("none")
                .telegramId("none")
                .discordId("none")
                .build();
        user.setId(0L);
        return user;

    }

    @Override
    public List<? extends DataNodeBase> findAll() {
        return this.dataValues;
    }


}
