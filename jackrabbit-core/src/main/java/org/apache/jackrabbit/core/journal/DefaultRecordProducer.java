/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.journal;

/**
 * Produces new records that can be appended to the journal.
 */
public class DefaultRecordProducer implements RecordProducer {

    /**
     * Journal.
     */
    private final AbstractJournal journal;

    /**
     * Producer identifier.
     */
    private String id;

    /**
     * Create a new instance of this class.
     *
     * @param journal journal
     * @param id producer id
     */
    public DefaultRecordProducer(AbstractJournal journal, String id) {
        this.journal = journal;
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    public Record append() throws JournalException {
        AppendRecord record = null;

        journal.lockAndSync();

        try {
            record = createRecord();
            journal.appending(record);
            return record;
        } finally {
            if (record == null) {
                journal.unlock(false);
            }
        }
    }

    /**
     * Create a new record. May be overridden by subclasses.
     *
     * @throws JournalException if an error occurs
     */
    protected AppendRecord createRecord() throws JournalException {
        return new AppendRecord(journal, id);
    }
}
