/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Dragon.models.skill;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Administrator
 */
@Setter
@Getter
public class SkillNotFocus extends Skill {

    private int timePre;

    private int timeDame;

    private short range;

    private long time;

    public SkillNotFocus(Skill skill) {
        super(skill);
        this.timePre = 2000;
        this.timeDame = 3000;
        this.range = 250;
    }
}
