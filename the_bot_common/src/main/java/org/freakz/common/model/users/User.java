package org.freakz.common.model.users;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.freakz.common.model.dto.DataNodeBase;

@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class User extends DataNodeBase {

  @JsonProperty("isAdmin")
  private boolean isAdmin;

  @JsonProperty("canDoIrcOp")
  private boolean canDoIrcOp;

  @JsonProperty("username")
  private String username;

  @JsonProperty("password")
  private String password;

  @JsonProperty("name")
  private String name;

  @JsonProperty("email")
  private String email;

  @JsonProperty("ircNick")
  private String ircNick;

  @JsonProperty("telegramId")
  private String telegramId;

  @JsonProperty("discordId")
  private String discordId;

  @JsonProperty("slackId")
  private String slackId;
}
