/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.openhft.chronicle.queue.impl.single;

import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireKey;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.WireUtil;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.UUID;

// TODO: is padded needed ?
class SingleChronicleQueueHeader implements Marshallable {

    private enum Fields implements WireKey {
        type,
        uuid, created, user, host,
        indexCount, indexSpacing,
        writePosition, dataPosition, index2Index, lastIndex
    }

    public static final String QUEUE_TYPE = "SCV4";
    public static final String CLASS_ALIAS = "Header";
    public static final long PADDED_SIZE = 512;

    // fields which can be serialized/deserialized in the normal way.
    private String type;
    private UUID uuid;
    private ZonedDateTime created;
    private String user;
    private String host;
    private int indexCount;
    private int indexSpacing;

    // support binding to off heap memory with thread safe operations.
    private LongValue writePosition;
    private LongValue dataPosition;
    private LongValue index2Index;
    private LongValue lastIndex;

    SingleChronicleQueueHeader() {
        this.type = QUEUE_TYPE;
        this.uuid = UUID.randomUUID();
        this.created = ZonedDateTime.now();
        this.user = System.getProperty("user.name");
        this.host = WireUtil.hostName();

        this.indexCount = 128 << 10;
        this.indexSpacing = 64;

        // This is set to null as that it can pick up the right time the
        // first time it is used.
        this.writePosition = null;
        this.dataPosition = null;
        this.index2Index = null;
        this.lastIndex = null;
    }

    LongValue writePosition() {
        return writePosition;
    }

    LongValue index2Index() {
        return index2Index;
    }

    LongValue lastIndex() {
        return lastIndex;
    }

    @Override
    public void writeMarshallable(@NotNull WireOut out) {
        out.write(Fields.type).text(type)
            .write(Fields.uuid).uuid(uuid)
            .write(Fields.writePosition).int64forBinding(WireUtil.SPB_HEADER_BYTE_SIZE)
            .write(Fields.dataPosition).int64forBinding(WireUtil.SPB_HEADER_BYTE_SIZE)
            .write(Fields.created).zonedDateTime(created)
            .write(Fields.user).text(user)
            .write(Fields.host).text(host)
            .write(Fields.indexCount).int32(indexCount)
            .write(Fields.indexSpacing).int32(indexSpacing)
            .write(Fields.index2Index).int64forBinding(0L)
            .write(Fields.lastIndex).int64forBinding(-1L);
        //out.addPadding((int) (PADDED_SIZE - out.bytes().writePosition()));
    }

    @Override
    public void readMarshallable(@NotNull WireIn in) {
        in.read(Fields.type).text(this, (o, i) -> o.type = i)
            .read(Fields.uuid).uuid(this, (o, i) -> o.uuid = i)
            .read(Fields.writePosition).int64(this.writePosition, this, (o, i) -> o.writePosition = i)
            .read(Fields.dataPosition).int64(this.dataPosition, this, (o, i) -> o.dataPosition = i)
            .read(Fields.created).zonedDateTime(this, (o, i) -> o.created = i)
            .read(Fields.user).text(this, (o, i) -> o.user = i)
            .read(Fields.host).text(this, (o, i) -> o.host = i)
            .read(Fields.indexCount).int32(this, (o, i) -> o.indexCount = i)
            .read(Fields.indexSpacing).int32(this, (o, i) -> o.indexSpacing = i)
            .read(Fields.index2Index).int64(this.index2Index, this, (o, i) -> o.index2Index = i)
            .read(Fields.lastIndex).int64(this.lastIndex, this, (o, i) -> o.lastIndex = i);
    }

    public long getWritePosition() {
        return this.writePosition.getVolatileValue();
    }

    public void setWritePosition(long writeByte) {
        this.writePosition.setOrderedValue(writeByte);
    }

    public long getDataPosition() {
        return this.dataPosition.getVolatileValue();
    }

    public void setDataPosition(long dataOffset) {
        this.dataPosition.setOrderedValue(dataOffset);
    }

    public long incrementLastIndex() {
        return lastIndex.addAtomicValue(1);
    }
}
